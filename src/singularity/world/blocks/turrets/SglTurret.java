package singularity.world.blocks.turrets;

import arc.Core;
import arc.audio.Sound;
import arc.func.Boolf;
import arc.func.Cons2;
import arc.graphics.Color;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.ui.layout.Table;
import arc.struct.EnumSet;
import arc.struct.ObjectMap;
import arc.struct.OrderedMap;
import arc.struct.Seq;
import arc.util.Strings;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.content.UnitTypes;
import mindustry.entities.*;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.pattern.ShootPattern;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.blocks.ControlBlock;
import mindustry.world.meta.*;
import singularity.world.blocks.SglBlock;
import singularity.world.consumers.SglConsumers;
import singularity.world.draw.DrawSglTurret;
import universecore.annotations.Annotations;
import universecore.components.blockcomp.ConsumerBuildComp;
import universecore.world.consumers.BaseConsumers;
import universecore.world.consumers.UncConsumeItems;

import java.util.concurrent.atomic.AtomicReference;

import static mindustry.Vars.tilesize;

@Annotations.ImplEntries
public class SglTurret extends SglBlock{
  private final int timerTarget = timers++;

  /**炮塔的索敌范围*/
  public float range = 80f;
  /**是否根据敌人的移动提前修正弹道*/
  public boolean accurateDelay = true;

  /**是否攻击空中目标*/
  public boolean targetAir = true;
  /**是否攻击地面目标*/
  public boolean targetGround = true;
  /**回复友方生命值*/
  public boolean targetHealing;
  /**单位目标选择过滤器*/
  public Boolf<Unit> unitFilter = u -> true;
  /**建筑目标选择过滤器*/
  public Boolf<Building> buildingFilter = b -> !b.block.underBullets;
  /**单位索敌排序准则，默认为最近目标*/
  public Units.Sortf unitSort = UnitSorts.closest;

  /**索敌时间间隔，以刻为单位*/
  public float targetInterval = 20;
  /**预热速度*/
  public float warmupSpeed = 0.1f;
  /**是否为线性预热过程*/
  public boolean linearWarmup = true;
  /**开火预热阈值，需要达到阈值才能开火*/
  public float fireWarmupThreshold = 0;

  /**开火音效*/
  public Sound shootSound = Sounds.shoot;
  /**充能音效*/
  public Sound chargeSound = Sounds.none;
  /**音效音量范围*/
  public float soundPitchMin = 0.9f, soundPitchMax = 1.1f;

  /**开火特效*/
  public Effect shootEffect;
  /**烟雾特效*/
  public Effect smokeEffect;
  /**弹药使用特效（例如抛壳）*/
  public Effect ammoUseEffect = Fx.none;
  /**炮管红热时的光效遮罩层颜色*/
  public Color heatColor = Pal.turretHeat;

  /**弹药出膛的偏移位置*/
  public float shootX = 0f, shootY = Float.NEGATIVE_INFINITY;
  /**子弹消耗特效产生的偏移位置*/
  public float ammoEjectBack = 1f;
  /**开火抖动*/
  public float shake = 0f;
  /**子弹最小开火范围，用于跨射武器*/
  public float minRange = 0f;
  /**弹药出膛位置的横向平移范围*/
  public float xRand = 0f;
  /**子弹弹道的散布角度范围*/
  public float inaccuracy = 0f;
  /**子弹速度的随机偏差量*/
  public float velocityRnd = 0f;
  /**炮塔的高光角度*/
  public float elevation = -1f;

  /**射击模式*/
  public ShootPattern shoot = new ShootPattern();
  /**炮管冷却时间，这仅用于绘制热量*/
  public float cooldownTime = 20f;
  /**后座力复位时间，默认使用当前弹药的装载时长*/
  public float recoilTime = -1;
  /**后座偏移插值的幂，参考{@link arc.math.Interp}*/
  public float recoilPow = 1.8f;
  /**炮塔尝试对目标开火的最小直线偏差角度*/
  public float shootCone = 8f;
  /**每次射击后座力最大平移距离*/
  public float recoil = 1f;
  /**转向速度*/
  public float rotateSpeed = 5;
  /**炮台在充能时能否转向*/
  public boolean moveWhileCharging = true;

  public ObjectMap<BaseConsumers, AmmoDataEntry> ammoTypes = new ObjectMap<>();

  public SglTurret(String name){
    super(name);
    update = true;
    solid = true;
    autoSelect = true;
    outlinedIcon = 1;
    quickRotate = false;
    outlineIcon = true;
    attacks = true;
    priority = TargetPriority.turret;
    group = BlockGroup.turrets;
    flags = EnumSet.of(BlockFlag.turret);

    draw = new DrawSglTurret();
  }

  @Override
  public void init(){
    if(shootY == Float.NEGATIVE_INFINITY) shootY = size * tilesize / 2f;
    if(elevation < 0) elevation = size / 2f;

    super.init();
  }

  public AmmoDataEntry newAmmo(BulletType ammoType){
    return newAmmo(ammoType, false, (t, p) -> {});
  }

  public AmmoDataEntry newAmmo(BulletType ammoType, Cons2<Table, BulletType> value){
    return newAmmo(ammoType, false, value);
  }

  public AmmoDataEntry newAmmo(BulletType ammoType, boolean override, Cons2<Table, BulletType> value){
    consume = new SglConsumers(false){
      {
        showTime = false;
      }

      @Override
      public BaseConsumers time(float time){
        showTime = false;
        craftTime = time;
        return this;
      }

      @Override
      public UncConsumeItems<? extends ConsumerBuildComp> items(ItemStack[] items){
        UncConsumeItems<? extends ConsumerBuildComp> res = new UncConsumeItems<>(items);
        res.showPerSecond = false;
        return add(res);
      }
    };
    consumers.add(consume);

    AmmoDataEntry res;
    ammoTypes.put(consume, res = new AmmoDataEntry(ammoType, override));
    res.display(value);

    return res;
  }

  @Override
  public void setConsumeStats(Stats stats){
    AtomicReference<Cons2<Table, BulletType>> buildRef = new AtomicReference<>();
    buildRef.set((ta, bullet) -> {
      ta.left().defaults().padRight(3).left();

      if(bullet.damage > 0 && (bullet.collides || bullet.splashDamage <= 0)){
        if(bullet.continuousDamage() > 0){
          ta.add(Core.bundle.format("bullet.damage", bullet.continuousDamage()) + StatUnit.perSecond.localized());
        }else{
          ta.add(Core.bundle.format("bullet.damage", bullet.damage));
        }
      }

      if(bullet.buildingDamageMultiplier != 1){
        sep(ta, Core.bundle.format("bullet.buildingdamage", (int) (bullet.buildingDamageMultiplier*100)));
      }

      if(bullet.rangeChange != 0){
        sep(ta, Core.bundle.format("bullet.range", (bullet.rangeChange > 0? "+": "-") + Strings.autoFixed(bullet.rangeChange/tilesize, 1)));
      }

      if(bullet.splashDamage > 0){
        sep(ta, Core.bundle.format("bullet.splashdamage", (int) bullet.splashDamage, Strings.fixed(bullet.splashDamageRadius/tilesize, 1)));
      }

      if(bullet.knockback > 0){
        sep(ta, Core.bundle.format("bullet.knockback", Strings.autoFixed(bullet.knockback, 2)));
      }

      if(bullet.healPercent > 0f){
        sep(ta, Core.bundle.format("bullet.healpercent", Strings.autoFixed(bullet.healPercent, 2)));
      }

      if(bullet.healAmount > 0f){
        sep(ta, Core.bundle.format("bullet.healamount", Strings.autoFixed(bullet.healAmount, 2)));
      }

      if(bullet.pierce || bullet.pierceCap != -1){
        sep(ta, bullet.pierceCap == -1? "@bullet.infinitepierce": Core.bundle.format("bullet.pierce", bullet.pierceCap));
      }

      if(bullet.incendAmount > 0){
        sep(ta, "@bullet.incendiary");
      }

      if(bullet.homingPower > 0.01f){
        sep(ta, "@bullet.homing");
      }

      if(bullet.lightning > 0){
        sep(ta, Core.bundle.format("bullet.lightning", bullet.lightning, bullet.lightningDamage < 0? bullet.damage: bullet.lightningDamage));
      }

      if(bullet.pierceArmor){
        sep(ta, "@bullet.armorpierce");
      }

      if(bullet.status != StatusEffects.none){
        sep(ta, (bullet.status.minfo.mod == null? bullet.status.emoji(): "") + "[stat]" + bullet.status.localizedName + "[lightgray] ~ [stat]" + ((int) (bullet.statusDuration/60f)) + "[lightgray] " + Core.bundle.get("unit.seconds"));
      }

      if(bullet.fragBullet != null){
        sep(ta, Core.bundle.format("bullet.frags", bullet.fragBullets));
        ta.row();
        ta.table(st -> buildRef.get().get(st, bullet.fragBullet));
      }

      ta.row();
    });

    stats.add(Stat.ammo, table -> {
      table.defaults().padLeft(15);
      for(ObjectMap.Entry<BaseConsumers, AmmoDataEntry> entry: ammoTypes){
        Stats stat = new Stats();
        stat.add(Stat.reload, 60f/entry.key.craftTime*shoot.shots, StatUnit.perSecond);
        entry.key.display(stat);

        table.row();
        table.table(t -> {
          t.defaults().left();
          t.table(st -> {
            for(OrderedMap<Stat, Seq<StatValue>> map: stat.toMap().values()){
              for(ObjectMap.Entry<Stat, Seq<StatValue>> statSeqEntry: map){
                st.table(s -> {
                  s.left();
                  s.add("[lightgray]" + statSeqEntry.key.localized() + ":[] ").left();
                  for(StatValue statValue: statSeqEntry.value){
                    statValue.display(s);
                    s.add().size(10.0F);
                  }
                }).left();
                st.row();
              }
            }
          });
          t.row();

          AmmoDataEntry ammoEntry = entry.value;
          BulletType type = ammoEntry.bulletType;

          if(type.spawnUnit != null && type.spawnUnit.weapons.size > 0){
            StatValues.ammo(ObjectMap.of(t, type.spawnUnit.weapons.first().bullet), 0).display(t);
            return;
          }

          t.table(bt -> {
            if(!ammoEntry.override){
              buildRef.get().get(bt, type);
            }

            for(Cons2<Table, BulletType> value: ammoEntry.statValues){
              value.get(bt, type);
            }
          }).padTop(-9).padLeft(0).left().get().background(Tex.underline);
        });
      }
    });
  }

  @Override
  public void drawPlace(int x, int y, int rotation, boolean valid){
    super.drawPlace(x, y, rotation, valid);
    Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, Pal.placing);
  }

  private static void sep(Table table, String text){
    table.row();
    table.add(text);
  }

  @Annotations.ImplEntries
  public class SglTurretBuild extends SglBuilding implements ControlBlock{
    public Vec2 recoilOffset = new Vec2();
    public float charge;
    public float reloadCounter;
    public float coolantScl;
    public int coolantSclTimer;
    public float warmup;
    public Posc target;
    public float rotation = 90;
    public Vec2 targetPos = new Vec2();
    public BlockUnitc unit = (BlockUnitc) UnitTypes.block.create(team);
    public BulletType currentAmmo;
    public float curRecoil;
    public float heat;

    public boolean wasShooting;

    int totalShots;
    int queuedBullets;

    @Override
    public SglTurret block(){
      return SglTurret.this;
    }

    @Override
    public Building create(Block block, Team team){
      super.create(block, team);
      if(!consumers.isEmpty()){
        recipeCurrent = 0;
        onUpdateCurrent();
      }

      return this;
    }

    @Override
    public void onUpdateCurrent(){
      super.onUpdateCurrent();
      if(!ammoTypes.containsKey(consumer.current)) throw new RuntimeException("unknown ammo recipe");
      currentAmmo = ammoTypes.get(consumer.current).bulletType;
    }

    @Override
    public void updateTile(){
      wasShooting = false;
      if(consumer.current == null) return;

      curRecoil = Mathf.approachDelta(curRecoil, 0, 1/(recoilTime > 0? recoilTime: consumer.current.craftTime));
      heat = Mathf.approachDelta(heat, 0, 1/cooldownTime);
      charge = charging() ? Mathf.approachDelta(charge, 1, 1/shoot.firstShotDelay) : 0;

      unit.tile(this);
      unit.rotation(rotation);
      unit.team(team);
      recoilOffset.trns(rotation, -Mathf.pow(curRecoil, recoilPow)*recoil);

      updateTraget();

      if(!isControlled()){
        unit.aimX(targetPos.x);
        unit.aimY(targetPos.y);
      }

      boolean tarValid = validateTarget();
      float targetRot = angleTo(targetPos);

      if(consumeValid() && (moveWhileCharging || !charging())){
        rotation = Angles.moveToward(rotation, targetRot, rotateSpeed*delta()*potentialEfficiency);
      }

      if(wasShooting() && consumeValid()){
        if(tarValid){
          warmup = linearWarmup? Mathf.approachDelta(warmup, 1, warmupSpeed*consEfficiency()):
              Mathf.lerpDelta(warmup, 1, warmupSpeed*consEfficiency());
          wasShooting = true;

          if(!charging() && warmup >= fireWarmupThreshold){
            if(reloadCounter >= consumer.current.craftTime){
              if(Angles.angleDist(rotation, targetRot) < shootCone){
                doShoot(currentAmmo);
              }
            }
          }
        }
      }else{
        warmup = linearWarmup? Mathf.approachDelta(warmup, 0, warmupSpeed): Mathf.lerpDelta(warmup, 0, warmupSpeed);
      }

      if(consumeValid() && reloadCounter < consumer.current.craftTime) reloadCounter += consEfficiency()*delta()*coolantScl;

      if(coolantSclTimer > 0) coolantSclTimer--;
      else coolantScl = 1;
    }

    protected boolean validateTarget(){
      return !Units.invalidateTarget(target, canHeal() ? Team.derelict : team, x, y) || isControlled();
    }

    @Override
    public boolean shouldConsume(){
      return super.shouldConsume() && consumer.current != null && reloadCounter < consumer.current.craftTime;
    }

    public void doShoot(BulletType type){
      float bulletX = x + Angles.trnsx(rotation - 90, shootX, shootY),
          bulletY = y + Angles.trnsy(rotation - 90, shootX, shootY);

      if(shoot.firstShotDelay > 0){
        chargeSound.at(bulletX, bulletY, Mathf.random(soundPitchMin, soundPitchMax));
        type.chargeEffect.at(bulletX, bulletY, rotation);
      }

      shoot.shoot(totalShots, (xOffset, yOffset, angle, delay, mover) -> {
        queuedBullets++;
        if(delay > 0f){
          Time.run(delay, () -> bullet(type, xOffset, yOffset, angle, mover));
        }else{
          bullet(type, xOffset, yOffset, angle, mover);
        }
        totalShots++;
      });

      reloadCounter %= consumer.current.craftTime;
      consumer.trigger();
    }

    protected void bullet(BulletType type, float xOffset, float yOffset, float angleOffset, Mover mover){
      queuedBullets--;

      if(dead) return;

      float xSpread = Mathf.range(xRand),
          bulletX = x + Angles.trnsx(rotation - 90, shootX + xOffset + xSpread, shootY + yOffset),
          bulletY = y + Angles.trnsy(rotation - 90, shootX + xOffset + xSpread, shootY + yOffset),
          shootAngle = rotation + angleOffset + Mathf.range(inaccuracy);

      float lifeScl = type.scaleLife ? Mathf.clamp(Mathf.dst(bulletX, bulletY, targetPos.x, targetPos.y) / type.range, minRange / type.range, range() / type.range) : 1f;

      handleBullet(type.create(this, team, bulletX, bulletY, shootAngle, -1f, (1f - velocityRnd) + Mathf.random(velocityRnd), lifeScl, null, mover, targetPos.x, targetPos.y), xOffset, yOffset, shootAngle - rotation);

      (shootEffect == null ? type.shootEffect : shootEffect).at(bulletX, bulletY, rotation + angleOffset, type.hitColor);
      (smokeEffect == null ? type.smokeEffect : smokeEffect).at(bulletX, bulletY, rotation + angleOffset, type.hitColor);
      shootSound.at(bulletX, bulletY, Mathf.random(soundPitchMin, soundPitchMax));

      ammoUseEffect.at(
          x - Angles.trnsx(rotation, ammoEjectBack),
          y - Angles.trnsy(rotation, ammoEjectBack),
          rotation * Mathf.sign(xOffset)
      );

      if(shake > 0){
        Effect.shake(shake, shake, this);
      }

      curRecoil = 1f;
      heat = 1f;
    }

    protected void handleBullet(Bullet bullet, float offsetX, float offsetY, float angleOffset){
    }

    public boolean charging(){
      return queuedBullets > 0 && shoot.firstShotDelay > 0;
    }

    public boolean wasShooting(){
      return isControlled()? unit.isShooting(): target != null;
    }

    public void updateTraget(){
      if(timer(timerTarget, targetInterval)){
        findTarget();
      }

      if(isControlled()){
        targetPos.set(unit.aimX(), unit.aimY());
      }
      else{
        targetPosition(target);

        if(Float.isNaN(rotation)) rotation = 0;
      }
    }

    public void targetPosition(Posc pos){
      if(!consumeValid() || pos == null) return;

      Vec2 offset = Tmp.v1.setZero();

      if(accurateDelay && pos instanceof Hitboxc h){
        offset.set(h.deltaX(), h.deltaY()).scl(shoot.firstShotDelay / Time.delta);
      }

      targetPos.set(Predict.intercept(this, pos, offset.x, offset.y, currentAmmo.speed <= 0.01f ? 99999999f : currentAmmo.speed));

      if(targetPos.isZero()){
        targetPos.set(pos);
      }
    }

    public float range(){
      if(currentAmmo != null){
        return range + currentAmmo.rangeChange;
      }
      return range;
    }

    public void findTarget(){
      float range = range();

      if(targetAir && !targetGround){
        target = Units.bestEnemy(team, x, y, range, e -> !e.dead() && !e.isGrounded() && unitFilter.get(e), unitSort);
      }else{
        target = Units.bestTarget(team, x, y, range, e -> !e.dead() && unitFilter.get(e) && (e.isGrounded() || targetAir) && (!e.isGrounded() || targetGround), b -> targetGround && buildingFilter.get(b), unitSort);

        if(target == null && canHeal()){
          target = Units.findAllyTile(team, x, y, range, b -> b.damaged() && b != this);
        }
      }
    }

    protected boolean canHeal(){
      return targetHealing && consumeValid() && currentAmmo.collidesTeam && currentAmmo.heals();
    }

    @Override
    public void drawSelect(){
      super.drawSelect();

      Drawf.dashCircle(x, y, range, Pal.placing);
    }

    @Override
    public float warmup(){
      return warmup;
    }

    @Override
    public float drawrot(){
      return rotation - 90;
    }

    @Override
    public Unit unit(){
      unit.tile(this);
      unit.team(team);
      return (Unit)unit;
    }
  }

  static class AmmoDataEntry{
    final BulletType bulletType;
    final boolean override;

    final Seq<Cons2<Table, BulletType>> statValues = new Seq<>();

    public AmmoDataEntry(BulletType bulletType, boolean override){
      this.override = override;
      this.bulletType = bulletType;
    }

    public AmmoDataEntry display(Cons2<Table, BulletType> statValue){
      statValues.add(statValue);
      return this;
    }
  }
}

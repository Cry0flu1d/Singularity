package singularity.type;

import arc.func.Cons;
import arc.func.Func2;
import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.ctype.ContentType;
import mindustry.ctype.MappableContent;
import mindustry.ctype.UnlockableContent;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.ui.ItemDisplay;
import mindustry.ui.LiquidDisplay;
import singularity.Sgl;
import singularity.ui.tables.GasDisplay;
import singularity.world.blockComp.HeatBuildComp;
import singularity.world.meta.SglStat;
import singularity.world.meta.SglStatUnit;
import singularity.world.reaction.ReactContainer;

import java.util.Locale;

import static singularity.Singularity.getModAtlas;

public class Reaction<R1 extends UnlockableContent, R2 extends UnlockableContent, P extends UnlockableContent> extends UnlockableContent{
  public Participant<R1> reactantA;
  public Participant<R2> reactantB;
  public Participant<P> product;
  
  public float deltaHeat;
  public float requireTemperature;
  public float requirePressure;
  
  public float reactTime = 60;
  public Cons<ReactContainer> reacting = e -> {};
  public Cons<ReactContainer> finished = e -> {};
  
  public Func2<Float, Float, Float> rateScl = (pressure, temperature) -> {
    int pressureSclBase = getGas().length - (product.isGas? 1: 0);
    int heatSclBase = deltaHeat > 0? 1: -1;
    
    return (pressure*pressureSclBase/2)+((float)Math.log(temperature)*heatSclBase/2);
  };
  
  public boolean itemReaction = false;
  public boolean liquidReaction = false;
  public boolean gasReaction = false;
  
  private byte iReactionCount = -1;
  private byte lReactionCount = -1;
  private byte gReactionCount = -1;
  
  public Reaction(Participant<R1> a, Participant<R2> b, Participant<P> out){
    super(a.getName() + "+" + b.getName() + "->" + out.getName());
    localizedName = a.amount + a.get().localizedName + " + " + b.amount + b.get().localizedName + " -> " + out.amount + out.get().localizedName;
    
    reactantA = a;
    reactantB = b;
    product = out;
    
    float consHeat = 0;
    
    consHeat += getHeat(a.get());
    consHeat += getHeat(b.get());
    
    deltaHeat = getHeat(out.get()) - consHeat;
  }
  
  public Reaction(R1 a, float b, R2 c, float d, P e, float f){
    this(new Participant<>(a, b), new Participant<>(c, d), new Participant<>(e, f));
  }
  
  private static float getHeat(MappableContent target){
    if(target instanceof Liquid){
      return ((Liquid) target).temperature*((Liquid) target).heatCapacity;
    }
  
    if(target instanceof Gas){
      return ((Gas) target).temperature*((Gas) target).heatCapacity;
    }
  
    return 0;
  }
  
  /**自定义项目*/
  public Reaction(String name){
    super(name);
  }
  
  @Override
  public void init(){
    super.init();
    Time.run(0, () -> Sgl.reactions.signupReaction(this));
    if(reactantA.reactant instanceof Item) iReactionCount++;
    if(reactantB.reactant instanceof Item) if(++iReactionCount >= 1) itemReaction = true;
    if(reactantA.reactant instanceof Liquid) lReactionCount++;
    if(reactantB.reactant instanceof Liquid) if(++lReactionCount >= 1) liquidReaction = true;
    if(reactantA.reactant instanceof Gas) gReactionCount++;
    if(reactantB.reactant instanceof Gas) if(++gReactionCount >= 1) gasReaction = true;
  }
  
  @Override
  public void setStats(){
    stats.useCategories = true;
    stats.add(SglStat.requirePressure, requirePressure*100, SglStatUnit.kPascal);
    stats.add(SglStat.requireTemperature, HeatBuildComp.getTemperature(requireTemperature), SglStatUnit.temperature);
    stats.add(SglStat.deltaHeat, deltaHeat/1000, SglStatUnit.kHeat);
    stats.add(SglStat.consume, t -> {
      t.row();
      setInfo(reactantA, t);
      t.row();
      setInfo(reactantB, t);
    });
    stats.add(SglStat.product, t -> {
      t.row();
      setInfo(product, t);
    });
  }
  
  protected void setInfo(Participant<?> part, Table table){
    table.table(t -> {
      t.defaults().left().fill().padLeft(6);
      if(part.isItem) t.add(new ItemDisplay(part.getItem(), (int)part.amount, reactTime, true));
      if(part.isLiquid) t.add(new LiquidDisplay(part.getLiquid(), part.amount/reactTime*60, true));
      if(part.isGas) t.add(new GasDisplay(part.getGas(), part.amount/reactTime*60, true, true));
    }).left().padLeft(5);
  }
  
  @Override
  public void loadIcon(){
    fullIcon = uiIcon = getModAtlas("reaction");
  }
  
  public Participant<?>[] getAllPart(){
    return new Participant[]{reactantA, reactantB, product};
  }
  
  public boolean accept(MappableContent target){
    return reactantA.reactant == target || reactantB.reactant == target;
  }
  
  @SuppressWarnings("unchecked")
  public Participant<Item>[] getItem(){
    if(iReactionCount == -1) return new Participant[]{};
    if(itemReaction) return new Participant[]{reactantA, reactantB};
    Participant<?>[] result = new Participant[1];
    if(reactantA.reactant instanceof Item){
      result[0] = reactantA;
    }
    else result[0] = reactantB;
    return (Participant<Item>[])result;
  }
  
  @SuppressWarnings("unchecked")
  public Participant<Liquid>[] getLiquid(){
    if(lReactionCount == -1) return new Participant[]{};
    if(liquidReaction) return new Participant[]{reactantA, reactantB};
    Participant<?>[] result = new Participant[1];
    if(reactantA.reactant instanceof Liquid){
      result[0] = reactantA;
    }
    else result[0] = reactantB;
    return (Participant<Liquid>[])result;
  }
  
  @SuppressWarnings("unchecked")
  public Participant<Gas>[] getGas(){
    if(gReactionCount == -1) return new Participant[]{};
    if(gasReaction) return new Participant[]{reactantA, reactantB};
    Participant<?>[] result = new Participant[1];
    if(reactantA.reactant instanceof Gas){
      result[0] = reactantA;
    }
    else result[0] = reactantB;
    return (Participant<Gas>[])result;
  }
  
  @Override
  public ContentType getContentType(){
    return SglContents.reaction;
  }
  
  @Override
  public String toString(){
    return "reaction:" + id + " - " + reactantA + " + " + reactantB + "->" + product + "], requires:[ temperature:" + requireTemperature + ", pressure:" + requirePressure + "]";
  }
  
  public static class Participant<Type extends UnlockableContent>{
    public final Type reactant;
    public final Class<Type> clazz;
    public final float amount;
    
    public boolean isItem;
    public boolean isLiquid;
    public boolean isGas;
    
    @SuppressWarnings("unchecked")
    public Participant(Type reactant, float amount){
      if(reactant instanceof Item || reactant instanceof Liquid || reactant instanceof Gas){
        isItem = reactant instanceof Item;
        isLiquid = reactant instanceof Liquid;
        isGas = reactant instanceof Gas;
        this.reactant = reactant;
        this.clazz = (Class<Type>) reactant.getClass();
        this.amount = amount;
      }
      else{
        throw new RuntimeException("Error participant! class >" + reactant.getClass().getName() + "< can not apply to reaction");
      }
    }
    
    public Type get(){
      return reactant;
    }
    
    public Item getItem(){
      return isItem? (Item)get(): null;
    }
    
    public Liquid getLiquid(){
      return isLiquid? (Liquid)get(): null;
    }
    
    public Gas getGas(){
      return isGas? (Gas)get(): null;
    }
  
    @Override
    public String toString(){
      return "[" + reactant.getContentType().name() + "]" + reactant.name + "*" + (isItem? (int)amount: amount);
    }
    
    public String getName(){
      String name = reactant.name.replace(Sgl.modName + "-", "");
      return amount + name.substring(0, 1).toUpperCase(Locale.ROOT) + name.charAt(1);
    }
  }
}

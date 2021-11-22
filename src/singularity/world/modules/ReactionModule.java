package singularity.world.modules;

import arc.func.Cons2;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.ctype.UnlockableContent;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.modules.BlockModule;
import singularity.Sgl;
import singularity.type.Gas;
import singularity.type.Reaction;
import singularity.type.SglContents;
import singularity.world.reaction.ReactContainer;

public class ReactionModule extends BlockModule{
  protected final ReactContainer entity;
  protected ObjectMap<Reaction<?, ?, ?>, float[]> reactions = new ObjectMap<>();
  protected ObjectSet<UnlockableContent> matched = new ObjectSet<>();
  
  public ReactionModule(ReactContainer entity){
    this.entity = entity;
  }
  
  public void matchAll(UnlockableContent target){
    if(!matched.add(target)) return;
    
    entity.items().each((i, n) -> {
      if(n > 0 && i != target){
        Reaction<?, ?, ?> r = Sgl.reactions.match(target, i);
        if(r != null && !reactions.containsKey(r)){
          reactions.put(r, new float[]{0});
        }
      }
    });
    
    entity.liquids().each((l, n) -> {
      if(n > 0 && l != target){
        Reaction<?, ?, ?> r = Sgl.reactions.match(target, l);
        if(r != null && !reactions.containsKey(r)){
          reactions.put(r, new float[]{0});
        }
      }
    });
    
    entity.gases().each(stack -> {
      if(stack.amount > 0 && stack.gas != target){
        Reaction<?, ?, ?> r = Sgl.reactions.match(target, stack.gas);
        if(r != null && !reactions.containsKey(r)){
          reactions.put(r, new float[]{0});
        }
      }
    });
  }
  
  public void update(){
    for(ObjectMap.Entry<Reaction<?, ?, ?>, float[]> react: reactions){
      if(metalValid(react.key, entity) && requireValid(react.key, entity)){
        float efficiencyScl = Math.max(1 + react.key.rateScl.get(entity.pressure(), entity.absTemperature())*Time.delta, 0);
        float reactTime = react.key.reactTime;
  
        for(Reaction.Participant<Liquid> part: react.key.getLiquid()){
          entity.liquids().remove(part.reactant, part.amount/reactTime*efficiencyScl);
        }
        for(Reaction.Participant<Gas> part: react.key.getGas()){
          entity.gases().remove(part.reactant, part.amount/reactTime*efficiencyScl);
        }
  
        if(react.key.product.isLiquid){
          entity.liquids().add((Liquid)react.key.product.reactant, react.key.product.amount/reactTime*efficiencyScl);
        }
        else if(react.key.product.isGas){
          entity.gases().add((Gas)react.key.product.reactant, react.key.product.amount/reactTime*efficiencyScl);
        }
  
        entity.handleHeat(-react.key.deltaHeat/reactTime*efficiencyScl);
        
        react.key.reacting.get(entity);
        
        react.value[0] += 1/react.key.reactTime*efficiencyScl;
        if(react.value[0] >= 1){
          for(Reaction.Participant<Item> part: react.key.getItem()){
            entity.items().remove(part.reactant, (int)part.amount);
          }
  
          if(react.key.product.isItem){
            entity.items().add((Item)react.key.product.reactant, (int)react.key.product.amount);
          }
  
          react.key.finished.get(entity);
          
          react.value[0] = 0;
        }
      }
      else{
        if(react.value[0] > 0.0001) react.value[0] -= react.value[0]*0.0001;
      }
      
      if(!metalValid(react.key, entity) && react.value[0] <= 0.0001){
        reactions.remove(react.key);
        matched.remove(react.key.reactantA.get());
        matched.remove(react.key.reactantB.get());
      }
    }
  }
  
  public boolean requireValid(Reaction<?, ?, ?> react, ReactContainer entity){
    return entity.absTemperature() >= react.requireTemperature && entity.pressure() >= react.requirePressure;
  }
  
  public boolean metalValid(Reaction<?, ?, ?> react, ReactContainer entity){
    float efficiencyScl = react.rateScl.get(entity.pressure(), entity.absTemperature())*Time.delta;
    float reactTime = react.reactTime;
  
    Reaction.Participant<Item>[] rItems = react.getItem();
    Reaction.Participant<Liquid>[] rLiquid = react.getLiquid();
    Reaction.Participant<Gas>[] rGas = react.getGas();
  
    for(Reaction.Participant<Item> part: rItems){
      if(entity.items().get(part.reactant) < part.amount) return false;
    }
    for(Reaction.Participant<Liquid> part: rLiquid){
      if(entity.liquids().get(part.reactant) < part.amount/reactTime*efficiencyScl) return false;
    }
    for(Reaction.Participant<Gas> part: rGas){
      if(entity.gases().get(part.reactant) < part.amount/reactTime*efficiencyScl) return false;
    }
    
    return true;
  }
  
  public boolean any(){
    return reactions.size > 0;
  }
  
  public void each(Cons2<Reaction<?, ?, ?>, Float> cons){
    reactions.each((r, a) -> cons.get(r, a[0]));
  }
  
  @Override
  public void read(Reads read){
    int length = read.i();
  
    for(int i = 0; i < length; i++){
      reactions.put(SglContents.reaction(read.i()), new float[]{read.f()});
    }
  }
  
  @Override
  public void write(Writes write){
    write.i(reactions.size);
    
    for(ObjectMap.Entry<Reaction<?, ?, ?>, float[]> react: reactions){
      write.i(react.key.id);
      write.f(react.value[0]);
    }
  }
}

package singularity.world.distribution.request;

import arc.struct.Seq;
import mindustry.gen.Building;
import mindustry.type.ItemStack;
import singularity.world.components.distnet.DistMatrixUnitBuildComp;
import singularity.world.distribution.DistBuffers;
import singularity.world.distribution.DistributeNetwork;
import singularity.world.distribution.GridChildType;
import singularity.world.distribution.MatrixGrid;
import singularity.world.distribution.buffers.ItemsBuffer;

/**从网络中读取物品，此操作将物品从网络缓存读出并写入到目标缓存，网络缓存会优先提供已缓存物品，若不足则从网络子容器申请物品到网络缓存再分配*/
public class ReadItemsRequest extends DistRequestBase<ItemStack>{
  private static final Seq<MatrixGrid.BuildingEntry<Building>> temp = new Seq<>();
  private final ItemsBuffer destination;
  private ItemsBuffer source;
  
  private final Seq<ItemStack> reqItems;
  private static final Seq<ItemStack> tempItems = new Seq<>();
  
  public ReadItemsRequest(DistMatrixUnitBuildComp sender, ItemsBuffer destination, Seq<ItemStack> items){
    super(sender);
    this.destination = destination;
    reqItems = items;
  }
  
  @Override
  public int priority(){
    return 128;
  }
  
  @Override
  public void init(DistributeNetwork target){
    super.init(target);
    source = target.getCore().distCore().getBuffer(DistBuffers.itemBuffer);
  }
  
  @Override
  public boolean preHandle(){
    tempItems.clear();
    
    for(ItemStack stack : reqItems){
      int req = stack.amount - source.get(stack.item);
      if(req > 0){
        tempItems.add(new ItemStack(stack.item, req));
      }
    }

    itemFor: for(ItemStack stack : tempItems){
      for(MatrixGrid grid : target.grids){
        for(MatrixGrid.BuildingEntry<Building> entry: grid.get(GridChildType.container,
            (e, c) -> e.items.get(stack.item) > 0 && c.get(GridChildType.container, stack.item),
            temp)){
          if(stack.amount <= 0) continue itemFor;
          if(source.remainingCapacity().intValue() <= 0) break itemFor;

          int move = Math.min(entry.entity.items.get(stack.item), stack.amount);
          move = Math.min(move, source.remainingCapacity().intValue());

          if(move > 0){
            move = entry.entity.removeStack(stack.item, move);
            source.put(stack.item, move);
            stack.amount -= move;
          }
        }
      }
    }
    
    return true;
  }
  
  @Override
  public boolean handle(){
    boolean blockTest = false;
    for(ItemStack stack : reqItems){
      int move = Math.min(stack.amount, source.get(stack.item));
      move = Math.min(move, destination.remainingCapacity().intValue());
      if(move <= 0) continue;
      
      source.remove(stack.item, move);
      destination.put(stack.item, move);
      blockTest = true;
    }
    return blockTest;
  }
  
  @Override
  public Seq<ItemStack> getList(){
    return reqItems;
  }
}
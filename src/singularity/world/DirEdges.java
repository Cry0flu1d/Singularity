package singularity.world;

import arc.func.Floatc2;
import arc.math.Mathf;
import arc.math.geom.Point2;
import mindustry.Vars;
import mindustry.world.Tile;

import java.util.Arrays;

import static mindustry.Vars.maxBlockSize;

public class DirEdges{
  private static final Point2[][][] edges = new Point2[Vars.maxBlockSize + 1][4][0];

  static {
    edges[0] = new Point2[4][0];

    for(int size = 1; size < Vars.maxBlockSize; size++){
      int off = (size + 1)%2;
      int rad = size/2;
      int minOff = -rad + off, maxOff = rad;

      for(int dir = 0; dir < 4; dir++){
        edges[size][dir] = new Point2[size];
        for(int m = minOff; m <= maxOff; m++){
          switch(dir){
            case 0 -> edges[size][dir][m + rad - off] = new Point2(rad + 1, m);
            case 1 -> edges[size][dir][m + rad - off] = new Point2(m, rad + 1);
            case 2 -> edges[size][dir][m + rad - off] = new Point2(-rad - 1 + off, m);
            case 3 -> edges[size][dir][m + rad - off] = new Point2(m, -rad - 1 + off);
          }
        }

        Arrays.sort(edges[size][dir], (a, b) -> Float.compare(Mathf.angle(a.x, a.y), Mathf.angle(b.x, b.y)));
      }
    }
  }

  public static Point2[] get(int size, int direction){
    if(size < 0 || size > maxBlockSize) throw new RuntimeException("Block size must be between 0 and " + maxBlockSize);

    return edges[size][Mathf.mod(direction, 4)];
  }

  public static void eachDirPos(Tile tile, int direction, Floatc2 posCons){
    Point2[] arr = get(tile.block().size, direction);

    for(Point2 p: arr){
      posCons.get(tile.x + p.x, tile.y + p.y);
    }
  }
}
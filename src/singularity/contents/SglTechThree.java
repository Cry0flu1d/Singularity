package singularity.contents;

import arc.struct.Seq;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.content.Planets;
import mindustry.game.Objectives;

import static mindustry.content.Blocks.*;
import static mindustry.content.Items.*;
import static mindustry.content.Liquids.hydrogen;
import static mindustry.content.Liquids.ozone;
import static singularity.contents.CrafterBlocks.*;
import static singularity.contents.DefenceBlocks.*;
import static singularity.contents.DistributeBlocks.*;
import static singularity.contents.LiquidBlocks.*;
import static singularity.contents.NuclearBlocks.*;
import static singularity.contents.ProductBlocks.*;
import static singularity.contents.SglItems.*;
import static singularity.contents.SglLiquids.*;
import static universecore.util.TechTreeConstructor.*;

public class SglTechThree implements ContentList{

  @Override
  public void load(){
    {//serpulo
      currentRoot(Planets.serpulo.techTree);

      node(laserDrill, rock_drill, rockD -> {
        rockD.node(ore_washer, oreWa -> {});
      });

      node(liquidContainer, liquid_unloader, liquidUnl -> {});

      node(platedConduit, cluster_conduit, cluCon -> {
        cluCon.node(conduit_riveting, conRiv -> {});
      });

      node(cryofluidMixer, FEX_phase_mixer, FEXMixer -> {});

      node(cultivator, incubator, incB -> {});

      node(cultivator, culturing_barn, culB -> {});

      node(phaseWeaver, fission_weaver, fisWea -> {});

      node(melter, thermal_centrifuge, theCen -> {
        theCen.node(laser_resolver, lasRes -> {});
      });

      node(siliconSmelter, distill_purifier, disPur -> {
        disPur.node(osmotic_purifier, osmPur -> {});
      });

      node(siliconSmelter, combustion_chamber, comCha -> {
        comCha.node(retort_column, retCol -> {});

        comCha.node(reacting_pool, reaPoo -> {
          reaPoo.node(electrolytor, ele -> {});

          reaPoo.node(osmotic_separation_tank, osmTank -> {});

          reaPoo.node(vacuum_crucible, vacCru -> {});
        });
      });

      node(melter, thermal_smelter, theCen -> {});

      node(graphitePress, crystallizer, cry -> {
        cry.node(FEX_crystal_charger, charger -> {
          charger.node(lattice_constructor, latCons -> {});
        });
      });

      node(coreShard, decay_bin, decBin -> {
        decBin.node(nuclear_pipe_node, nucNode -> {
          nucNode.node(phase_pipe_node, phaseNode -> {

          });

          nucNode.node(fuel_packager, fuelPack -> {});

          nucNode.node(nuclear_reactor, nucReact -> {
            nucReact.node(lattice_reactor, latReact -> {
              latReact.node(overrun_reactor, oveReact -> {});
            });

            nucReact.node(nuclear_impact_reactor, nucImp -> {});

            nucReact.node(neutron_generator, neutGen -> {});
          });
        });
      });

      node(blastDrill, tidal_drill, tidDil -> {
        tidDil.node(force_field_extender, forExt -> {});
      });

      node(blastDrill, matrix_miner, Seq.with(new Objectives.Research(matrix_core)), matDil -> {
        matDil.node(matrix_miner_node, matNod -> {});
      });

      node(coreShard, matrix_bridge, matBri -> {
        matBri.node(matrix_core, matCore -> {
          matCore.node(matrix_process_unit, matProc -> {});

          matCore.node(matrix_topology_container, matTop -> {});

          matCore.node(matrix_component_interface, matComp -> {
            matComp.node(interface_jump_line, jump -> {});

            matComp.node(matrix_buffer, buff -> {});
          });

          matCore.node(matrix_energy_manager, matEnm -> {
            matEnm.node(matrix_energy_buffer, matEnb -> {});

            matEnm.node(matrix_power_interface, matPoi -> {});

            matEnm.node(matrix_neutron_interface, matNui -> {});
          });

          matCore.node(matrix_controller, matCtrl -> {
            matCtrl.node(matrix_grid_node, matGnd -> {});
          });
        });
      });

      node(thoriumWall, strengthening_alloy_wall, strWall -> {
        strWall.node(strengthening_alloy_wall_large, strWallLarge -> {});

        strWall.node(neutron_polymer_wall, neuWall -> {
          neuWall.node(neutron_polymer_wall_large, neuWallLar -> {});
        });
      });

      nodeProduce(Items.sand, rock_bitumen, rockB -> {
        rockB.nodeProduce(FEX_liquid, FEXl -> {
          FEXl.nodeProduce(crystal_FEX, cryFEX -> {
            cryFEX.nodeProduce(crystal_FEX_power, powFEX -> {});
          });

          FEXl.nodeProduce(phase_FEX_liquid, phaFEX -> {});
        });
      });

      nodeProduce(Liquids.water, algae_mud, alMud -> {
        alMud.nodeProduce(chlorella_block, chBlock -> {
          chBlock.nodeProduce(chlorella, chl -> {});
        });
      });

      nodeProduce(lead, aluminium, alu -> {});

      nodeProduce(Liquids.water, ozone, alMud -> {});

      nodeProduce(Liquids.water, hydrogen, hyd -> {});

      nodeProduce(Liquids.water, purified_water, puW -> {
        puW.nodeProduce(flocculant, flo -> {});
      });

      nodeProduce(Items.sand, black_crystone, cruOre -> {
        cruOre.nodeProduce(mixed_ore_solution, oreSol -> {
          
        });
      });

      nodeProduce(Items.sand, uranium_rawore, uRaw -> {
        uRaw.nodeProduce(uranium_salt_solution, uraSol -> {
          uraSol.nodeProduce(uranium_rawmaterial, ura_raw -> {});
        });
      });

      nodeProduce(Items.sand, alkali_stone, alk -> {
        alk.nodeProduce(lye, lye -> {});

        alk.nodeProduce(chlorine, chl -> {});
      });

      nodeProduce(silicon, silicon_chloride_sol, scs -> {});

      nodeProduce(pyratite, acid, acd -> {});

      nodeProduce(pyratite, sulfur_dioxide, sufDie -> {});

      nodeProduce(sporePod, spore_cloud, spoClo -> {});

      nodeProduce(scrap, nuclear_waste, nucWes -> {
        nucWes.nodeProduce(iridium_mixed_rawmaterial, iriRaw -> {
          iriRaw.nodeProduce(iridium_chloride, iriChl -> {});
        });
      });

      nodeProduce(thorium, uranium_235, u235 -> {
        u235.nodeProduce(concentration_uranium_235, cu235 -> {});
      });

      nodeProduce(thorium, uranium_238, u238 -> {
        u238.nodeProduce(plutonium_239, p239 -> {
          p239.nodeProduce(concentration_plutonium_239, cp239 -> {});
        });
      });

      nodeProduce(titanium, strengthening_alloy, strAlloy -> {
        strAlloy.nodeProduce(matrix_alloy, matAlloy -> {});
      });

      nodeProduce(titanium, iridium, iri -> {
        iri.nodeProduce(degenerate_neutron_polymer, neuPol -> {
          neuPol.nodeProduce(anti_metter, antMet -> {});
        });
      });

      nodeProduce(metaglass, aerogel, aGel -> {});

      nodeProduce(coal, coke, coke -> {});
    }

    {//erekir
      currentRoot(Planets.erekir.techTree);
    }
  }
}

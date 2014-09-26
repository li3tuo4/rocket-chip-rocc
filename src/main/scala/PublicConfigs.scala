// See LICENSE for license details.

package rocketchip

import Chisel._
import uncore._
import rocket._
import rocket.Util._

class DefaultConfig extends ChiselConfig {
  val topDefinitions:World.TopDefs = {
    (pname,site,here) => pname match {
      //RocketChip Parameters
      case BuildTile => (r:Bool) => {new RocketTile(resetSignal = r)}
      //HTIF Parameters
      case HTIFWidth => Dump("HTIF_WIDTH", 16)
      case HTIFNSCR => 64
      case HTIFOffsetBits => site(CacheBlockOffsetBits)
      case HTIFNCores => site(NTiles)
      //Memory Parameters
      case PAddrBits => 32
      case VAddrBits => 43
      case PgIdxBits => 13
      case ASIdBits => 7
      case PermBits => 6
      case PPNBits => site(PAddrBits) - site(PgIdxBits)
      case VPNBits => site(VAddrBits) - site(PgIdxBits)
      case MIFTagBits => Dump("MEM_TAG_BITS", 5)
      case MIFDataBits => Dump("MEM_DATA_BITS", 128)
      case MIFAddrBits => Dump("MEM_ADDR_BITS", site(PAddrBits) - site(CacheBlockOffsetBits))
      case MIFDataBeats => site(TLDataBits)/site(MIFDataBits)
      //Params used by all caches
      case ECCCode => None
      case WordBits => site(XprLen)
      case Replacer => () => new RandomReplacement(site(NWays))
      case BlockOffBits => site(CacheName) match {
        case "L1I" | "L1D" => log2Up(site(TLDataBits)/8)
        case "L2" => 0
      }
      case NSets => site(CacheName) match {
        case "L1I" => 128
        case "L1D" => Knob("L1D_SETS") //128
        case "L2" => 512 
      }
      case NWays => site(CacheName) match {
        case "L1I" => 2
        case "L1D" => Knob("L1D_WAYS") //4
        case "L2" => 8
      }
      case RowBits => site(CacheName) match {
        case "L1I" => 4*site(CoreInstBits)
        case "L1D" => 2*site(CoreDataBits)
        case "L2" => site(TLDataBits)
      }
      //L1InstCache
      case NITLBEntries => 8
      case NBTBEntries => 62
      case NRAS => 2
      //L1DataCache
      case NDTLBEntries => 8
      case StoreDataQueueDepth => 17
      case ReplayQueueDepth => 16
      case NMSHRs => Knob("L1D_MSHRS")
      case LRSCCycles => 32 
      //L2CacheParams
      case NReleaseTransactors => Knob("L2_REL_XACTS")
      case NAcquireTransactors => Knob("L2_ACQ_XACTS")
      case NClients => site(NTiles) + 1
      //Tile Constants
      case BuildRoCC => None
      case NDCachePorts => 2 + (if(site(BuildRoCC).isEmpty) 0 else 1) 
      case NTilePorts => 2 + (if(site(BuildRoCC).isEmpty) 0 else 1)
      case NPTWPorts => 2 + (if(site(BuildRoCC).isEmpty) 0 else 3)
      //Rocket Core Constants
      case RetireWidth => 1
      case UseVM => true
      case FastLoadWord => true
      case FastLoadByte => false
      case FastMulDiv => true
      case XprLen => 64
      case NMultXpr => 32
      case BuildFPU => Some(() => Module(new FPU))
      case SFMALatency => 2
      case DFMALatency => 3
      case CoreInstBits => 32
      case CoreDataBits => site(XprLen)
      case CoreDCacheReqTagBits => 7 + log2Up(here(NDCachePorts))
      //Uncore Paramters
      case LNMasters => site(NBanks)
      case LNClients => site(NTiles)+1
      case LNEndpoints => site(LNMasters) + site(LNClients)
      case TLId => "inner"
      case TLCoherence => site(Coherence)
      case TLAddrBits => site(PAddrBits) - site(CacheBlockOffsetBits)
      case TLMasterXactIdBits => site(TLId) match {
        case "inner" => log2Up(site(NReleaseTransactors)+site(NAcquireTransactors))
        case "outer" => 1
      }
      case TLClientXactIdBits => site(TLId) match {
        case "inner" => log2Up(site(NMSHRs))+log2Up(site(NTilePorts))
        case "outer" => log2Up(site(NReleaseTransactors)+site(NAcquireTransactors))
      }
      case TLDataBits => site(CacheBlockBytes)*8
      case TLWriteMaskBits => 6
      case TLWordAddrBits  => 3
      case TLAtomicOpBits  => 4
      case NTiles => Knob("NTILES")
      case NBanks => Knob("NBANKS")
      case NOutstandingMemReqs => 16 //site(NBanks)*(site(NReleaseTransactors)+site(NAcquireTransactors))
      case BankIdLSB => 5
      case CacheBlockBytes => 64
      case CacheBlockOffsetBits => log2Up(here(CacheBlockBytes))
      case UseBackupMemoryPort => true
      case BuildCoherenceMaster => (id: Int) => {
          Module(new L2CoherenceAgent(id, "inner", "outer"), { case CacheName => "L2" })
      }
      case Coherence => new MSICoherence(() => new NullRepresentation)
    }
  }
  override val knobValues:Any=>Any = {
    case "NTILES" => 1
    case "NBANKS" => 1
    case "L2_REL_XACTS" => 1
    case "L2_ACQ_XACTS" => 7
    case "L1D_MSHRS" => 2
    case "L1D_SETS" => 128
    case "L1D_WAYS" => 4
  }
}
class DefaultVLSIConfig extends DefaultConfig
class DefaultCPPConfig extends DefaultConfig


class FPGAConfig(default: ChiselConfig) extends ChiselConfig {
  val topDefinitions:World.TopDefs = {
    (pname,site,here) => pname match {
      case UseBackupMemoryPort => false
      case _ => default.topDefinitions(pname,site,here)
    }
  }
  override val knobValues = default.knobValues
}

class DefaultFPGAConfig extends FPGAConfig(new DefaultConfig)


class FPGASmallConfig(default: ChiselConfig) extends ChiselConfig {
  val topDefinitions:World.TopDefs = {
    (pname,site,here) => pname match {
      case NSets => site(CacheName) match {
        case "L1I" => 64
        case "L1D" => Knob("L1D_SETS")
      }
      case NWays => site(CacheName) match {
        case "L1I" => 1
        case "L1D" => Knob("L1D_WAYS")
      }
      case BuildFPU => None
      case FastMulDiv => false
      case NITLBEntries => 4
      case NBTBEntries => 8
      case NDTLBEntries => 4
      case UseBackupMemoryPort => false
      case _ => default.topDefinitions(pname,site,here)
    }
  }
  override val knobValues:Any=>Any = {
    case "NTILES" => 1
    case "NBANKS" => 1
    case "L2_REL_XACTS" => 1
    case "L2_ACQ_XACTS" => 7
    case "L1D_MSHRS" => 2
    case "L1D_SETS" => 64
    case "L1D_WAYS" => 1
  }
}

class DefaultFPGASmallConfig extends FPGASmallConfig(new FPGAConfig(new DefaultConfig))
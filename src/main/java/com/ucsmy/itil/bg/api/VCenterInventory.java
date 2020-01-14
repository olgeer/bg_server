/*
package com.ucsmy.itil.bg.api;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Logger;

import com.ebay.odb.sync.ESXServerSync;
import com.vmware.vim25.AboutInfo;
import com.vmware.vim25.ClusterComputeResourceSummary;
import com.vmware.vim25.ClusterConfigInfo;
import com.vmware.vim25.DatastoreInfo;
import com.vmware.vim25.HostHardwareInfo;
import com.vmware.vim25.HostHostBusAdapter;
import com.vmware.vim25.HostInternetScsiHba;
import com.vmware.vim25.HostInternetScsiHbaSendTarget;
import com.vmware.vim25.HostInternetScsiTargetTransport;
import com.vmware.vim25.HostMultipathInfoLogicalUnit;
import com.vmware.vim25.HostMultipathInfoPath;
import com.vmware.vim25.HostNetworkInfo;
import com.vmware.vim25.HostPortGroup;
import com.vmware.vim25.HostScsiDiskPartition;
import com.vmware.vim25.HostStorageDeviceInfo;
import com.vmware.vim25.HostTargetTransport;
import com.vmware.vim25.HostVirtualNic;
import com.vmware.vim25.HostVirtualSwitch;
import com.vmware.vim25.HostVmfsVolume;
import com.vmware.vim25.HostNasVolume;
import com.vmware.vim25.PhysicalNic;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualHardware;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualMachineSummary;
import com.vmware.vim25.VmfsDatastoreInfo;
import com.vmware.vim25.NasDatastoreInfo;
import com.vmware.vim25.mo.ClusterComputeResource;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

public class VCenterInventory {
    private static final Logger log = Logger.getLogger(ESXServerSync.class);
    private String host;

    private String username;

    private String password;

    public VCenterInventory(String host, String username, String password) {
        super();
        this.host = host;
        this.username = username;
        this.password = password;
    }

    public VCenter getVCenterInfo() throws Exception {
        URL url = new URL("https", host, "/sdk");
        ServiceInstance si = new ServiceInstance(url, username, password, true);
        Folder rootFolder = si.getRootFolder();
        VCenter vc = new VCenter();
        AboutInfo ai = si.getAboutInfo();
        vc.setHostname(host);
        vc.setDescr(ai.getFullName());
        vc.setVersion(ai.getVersion());
        InventoryNavigator inav = new InventoryNavigator(rootFolder);
        ManagedEntity[] esxs = inav.searchManagedEntities("HostSystem");
        List<ESXServer> servers = new ArrayList<ESXServer>();
        for (ManagedEntity esx : esxs) {
            HostSystem hs = (HostSystem) esx;
            HostHardwareInfo hwi = hs.getHardware();
            long hz = hwi.cpuInfo.hz;
            long e9 = 1000000000;
            double hzd = new java.math.BigDecimal(((double) hz) / e9).setScale(2, java.math.BigDecimal.ROUND_HALF_UP)
                    .doubleValue();
            ESXServer server = new ESXServer();
            server.setName(hs.getName());
            server.setCpufrequency(String.valueOf(hzd) + "GHz");
            servers.add(server);
        }
        vc.setEsxservers(servers);

        ManagedEntity[] clusters = inav.searchManagedEntities("Datacenter");
        List<Cluster> cs = new ArrayList<Cluster>();
        for (ManagedEntity me : clusters) {
            Datacenter dc = (Datacenter) me;
            String path = dc.getName();
            cs.addAll(getClusters(dc, path, dc.getHostFolder()));
        }
        vc.setClusters(cs);
        si.getSessionManager().logout();
        return vc;
    }


    private static List<Cluster> getClusters(Datacenter dc, String path, Folder f) throws Exception {
        List<Cluster> cs = new ArrayList<Cluster>();
        ManagedEntity[] ces = f.getChildEntity();
        for (ManagedEntity ce : ces) {
            if (ce instanceof ClusterComputeResource) {
                cs.add(getClusterInfo(dc, path, (ClusterComputeResource) ce));
            } else if (ce instanceof Folder) {
                cs.addAll(getClusters(dc, path + ":" + ce.getName(), (Folder) ce));
            }
        }
        return cs;
    }

    public List<VMachine> getAllVM(String esxserver) throws Exception {
        ManagedEntity[] esxs = null;
        URL url = new URL("https", host, "/sdk");
        ServiceInstance si = new ServiceInstance(url, username, password, true);
        Folder rootFolder = si.getRootFolder();
        esxs = new InventoryNavigator(rootFolder).searchManagedEntities("HostSystem");
        for (ManagedEntity esx : esxs) {
            HostSystem hs = (HostSystem) esx;
            if (hs.getName().equals(esxserver)) {
                List<VMachine> vms = getAllVM(hs);
                si.getSessionManager().logout();
                return vms;
            }
        }
        si.getSessionManager().logout();
        throw new RuntimeException("Cannot find esxserver:" + esxserver);
    }

    public List<Datastore> getAllDatastores(String esxserver) throws Exception {
        ManagedEntity[] esxs = null;
        URL url = new URL("https", host, "/sdk");
        ServiceInstance si = new ServiceInstance(url, username, password, true);
        Folder rootFolder = si.getRootFolder();
        esxs = new InventoryNavigator(rootFolder).searchManagedEntities("HostSystem");
        for (ManagedEntity esx : esxs) {
            HostSystem hs = (HostSystem) esx;
            if (hs.getName().equals(esxserver)) {
                List<Datastore> ds = getAllDatastores(hs);
                si.getSessionManager().logout();
                return ds;
            }
        }
        si.getSessionManager().logout();
        throw new RuntimeException("Cannot find esxserver:" + esxserver);
    }

    public NetworkInfo getNetworkInfo(String esxserver) throws Exception {
        ManagedEntity[] esxs = null;
        URL url = new URL("https", host, "/sdk");
        ServiceInstance si = new ServiceInstance(url, username, password, true);
        Folder rootFolder = si.getRootFolder();
        esxs = new InventoryNavigator(rootFolder).searchManagedEntities("HostSystem");
        for (ManagedEntity esx : esxs) {
            HostSystem hs = (HostSystem) esx;
            if (hs.getName().equals(esxserver)) {
                NetworkInfo ni = getNetworkInfo(hs);
                si.getSessionManager().logout();
                return ni;
            }
        }
        si.getSessionManager().logout();
        throw new RuntimeException("Cannot find esxserver:" + esxserver);
    }

    private NetworkInfo getNetworkInfo(HostSystem esxserver) throws Exception {
        NetworkInfo ninfo = new NetworkInfo();
        HostNetworkInfo nwi = esxserver.getConfig().getNetwork();
        HostPortGroup[] portgroups = nwi.getPortgroup();
        Map<String, String> pgMap = new HashMap<String, String>();
        for (HostPortGroup portgroup : portgroups) {
            pgMap.put(portgroup.getKey(), portgroup.getSpec().getName());
        }
        PhysicalNic[] pnics = nwi.getPnic();
        Map<String, String> pnicMap = new HashMap<String, String>();
        for (PhysicalNic pnic : pnics) {
            pnicMap.put(pnic.getKey(), pnic.getMac());
        }
        List<VirtualSwitch> vss = new ArrayList<VirtualSwitch>();
        HostVirtualSwitch[] vswtichs = nwi.getVswitch();
        for (HostVirtualSwitch vswitch : vswtichs) {
            VirtualSwitch vs = new VirtualSwitch();
            vs.setName(vswitch.getName());
            String[] macKeys = vswitch.getPnic();
            if (macKeys != null) {
                for (String key : macKeys) {
                    vs.addPhysicalMAC(pnicMap.get(key));
                }
            }
            String[] pgs = vswitch.getPortgroup();
            if (pgs != null) {
                for (String pg : pgs) {
                    vs.addPortgroup(pgMap.get(pg));
                }
            }
// TODO
            vss.add(vs);
        }
        ninfo.setVss(vss);

        List<VirtualNic> vnics = new ArrayList<VirtualNic>();
        HostVirtualNic[] virtualnics = nwi.getVnic();
        for (HostVirtualNic virtualnic : virtualnics) {
            VirtualNic vnic = new VirtualNic();
            vnic.setName(virtualnic.getDevice());
            vnic.setPortgroup(virtualnic.getPortgroup());
            vnic.setMac(virtualnic.getSpec().getMac());
            vnics.add(vnic);
        }
        ninfo.setVnics(vnics);
        return ninfo;
    }

    private List<Datastore> getAllDatastores(HostSystem esxserver) throws Exception {
        List<Datastore> datastores = new ArrayList<Datastore>();

        Map<String, Disk> storages = new HashMap<String, Disk>();
        HostStorageDeviceInfo sd = esxserver.getConfig().getStorageDevice();
        HostHostBusAdapter[] hostBusAdapters = sd.getHostBusAdapter();
        Map<String, String[]> hostBuses = new HashMap<String, String[]>();
        for (HostHostBusAdapter adapter : hostBusAdapters) {
            if (adapter instanceof HostInternetScsiHba) {
                HostInternetScsiHbaSendTarget[] targets = ((HostInternetScsiHba) adapter).getConfiguredSendTarget();
                if (null != targets && targets.length > 0) {
                    String[] floatings = new String[targets.length];
                    for (int i = 0; i < targets.length; i++) {
                        floatings[i] = targets[i].getAddress();
                    }
                    hostBuses.put(adapter.getKey(), floatings);
                }
            }
        }
        HostMultipathInfoLogicalUnit[] luns = sd.getMultipathInfo().getLun();
        for (HostMultipathInfoLogicalUnit lun : luns) {
            HostMultipathInfoPath[] paths = lun.getPath();
            for (HostMultipathInfoPath path : paths) {
                String pathName = path.getName();
                String diskName = pathName.substring(pathName.lastIndexOf('-') + 1);
                Disk s = new Disk();
                s.setPath(pathName);
                s.setDiskName(diskName);
                String[] floatings = hostBuses.get(path.getAdapter());
                s.setAddresses(floatings);
                HostTargetTransport transport = path.getTransport();
                if (transport instanceof HostInternetScsiTargetTransport) {
                    s.setIScsiName(((HostInternetScsiTargetTransport) transport).iScsiName);
                }
                storages.put(diskName, s);
            }
        }

        com.vmware.vim25.mo.Datastore[] dss = esxserver.getDatastores();
        for (com.vmware.vim25.mo.Datastore ds : dss) {
            String name = ds.getName();
            DatastoreInfo info = ds.getInfo();
            HostVmfsVolume vmfs = null;
            HostNasVolume nasfs = null;
            if (info instanceof VmfsDatastoreInfo) {
                vmfs = ((VmfsDatastoreInfo) info).getVmfs();
//log.warn("get vmfs:"+vmfs.getName());
            } else if (info instanceof NasDatastoreInfo) {
                nasfs = ((NasDatastoreInfo) info).getNas();
//log.warn("get nasfs:"+nasfs.getName());
            } else {
                continue;
            }

            Datastore datastore = new Datastore();
            datastore.setName(name);
            datastore.setStatus(ds.getOverallStatus().name());
            datastore.setCapacity(ds.getSummary().getCapacity());
            datastore.setFreeSpace(info.getFreeSpace());
            datastore.setMaxFileSize(info.getMaxFileSize());
            datastore.setUrl(info.getUrl());
            datastore.setType(ds.getSummary().getType());
            if (vmfs != null) {
                datastore.setVersion(vmfs.getVersion());
                datastore.setVmfsUUID(vmfs.getUuid());
                HostScsiDiskPartition[] extents = vmfs.getExtent();

                for (HostScsiDiskPartition disk : extents) {
                    String diskName = disk.getDiskName();
                    datastore.addDisk(storages.get(diskName));
                }
            }
            VirtualMachine[] vms = ds.getVms();
            for (VirtualMachine vm : vms) {
                datastore.addVm(vm.getName());
            }
            datastores.add(datastore);

        }
        return datastores;
    }


    private List<VMachine> getAllVM(HostSystem esxserver) throws Exception {
        VirtualMachine[] vms = esxserver.getVms();

        List<VMachine> machines = new ArrayList<VMachine>();
        for (VirtualMachine vm : vms) {
            VirtualMachineConfigInfo config = vm.getConfig();
            VirtualMachineSummary summary = vm.getSummary();
            VMachine vmachine = new VMachine();
            vmachine.setName(vm.getName());
            vmachine.setUuid(config.getUuid());
            VirtualEthernetCard vec = getMac(config.getHardware());
            vmachine.setMac(vec.getMacAddress().toUpperCase());
            vmachine.setPortgroup(vec.getDeviceInfo().getSummary());
            vmachine.setVmPath(config.getFiles().getVmPathName());
            vmachine.setVmID(vm.getMOR().get_value());
            vmachine.setModel(config.getGuestFullName());
            vmachine.setPower(summary.getRuntime().getPowerState().name());
            vmachine.setHealth(vm.getOverallStatus().name());
            vmachine.setVersion(config.getVersion());
            vmachine.setVmIP(summary.getGuest().getIpAddress());
            vmachine.setCpucount(String.valueOf(vm.getSummary().getConfig().numCpu));
            machines.add(vmachine);
        }
        return machines;
    }

    private static VirtualEthernetCard getMac(VirtualHardware hardware) {
        VirtualDevice[] devices = hardware.getDevice();
        for (VirtualDevice device : devices) {
            if (device instanceof VirtualEthernetCard) {
                return ((VirtualEthernetCard) device);
            }
        }
        return null;
    }

    private static Cluster getClusterInfo(Datacenter dc, String path, ClusterComputeResource ccr) {
        Cluster cluster = new Cluster();
        cluster.setName(ccr.getName());
        cluster.setDatacenter(dc.getName());
        cluster.setPath(path);
        ClusterComputeResourceSummary summary = (ClusterComputeResourceSummary) ccr.getSummary();
        cluster.setCpu(summary.getTotalCpu());
        cluster.setCpucount(summary.getNumCpuCores());
        cluster.setMemory(summary.getTotalMemory());
        cluster.setEvcMode(summary.getCurrentEVCModeKey());
        ClusterConfigInfo config = ccr.getConfiguration();
        cluster.setHaEnabled(config.getDasConfig().getEnabled());
        cluster.setDrsEnabled(config.getDrsConfig().getEnabled());
        HostSystem[] hs = ccr.getHosts();
        for (HostSystem hostSystem : hs) {
            cluster.addServer(hostSystem.getName());
        }
        return cluster;
    }
}*/

# Copyright (c) 2014-2022 Embedded Systems and Applications, TU Darmstadt.
#
# This file is part of TaPaSCo
# (see https://github.com/esa-tu-darmstadt/tapasco).
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program. If not, see <http://www.gnu.org/licenses/>.
#

# if page migrations over the network are enabled, create Aurora core and connect NetworkPageDMA
# FIXME make network port configurable (see sfpplus feature as reference)
if {[tapasco::is_feature_enabled "SVM"] && [tapasco::get_feature_option "SVM" "network_dma"] == "true"} {
  proc create_custom_subsystem_network_svm {} {
    if {![svm::is_svm_supported]} {
      puts "ERROR: SVM not support on specified platform"
      exit 1
    }
    set mac_addr [tapasco::get_feature_option "SVM" "mac_addr"]
    if {$mac_addr == "false"} {
      puts "ERROR: parameter \"mac_addr\" required if network page migrations are enabled"
      exit 1
    }
    set port_no [tapasco::get_feature_option "SVM" "port"]
    if {$port_no == "false"} {
      puts "No port number for network DMA transfers given...assuming port 0"
      set port_no 0
    }
    if {![svm::is_network_port_valid $port_no]} {
      puts [format {ERROR: Invalid port number %s for network port specified} $port_no]
      exit 1
    }

    set pcie_aclk [tapasco::subsystem::get_port "host" "clk"]
    set pcie_p_aresetn [tapasco::subsystem::get_port "host" "rst" "peripheral" "resetn"]

    # create constants and clk_wiz
    set const_zero [tapasco::ip::create_constant const_zero_svm 1 0]
    set const_one [tapasco::ip::create_constant const_one_svm 1 1]

    set clk_wiz_svm [tapasco::ip::create_clk_wiz clk_wiz_svm]
    set_property -dict [list CONFIG.USE_SAFE_CLOCK_STARTUP {true} \
      CONFIG.CLKOUT1_REQUESTED_OUT_FREQ 100 \
      CONFIG.USE_LOCKED {false} \
      CONFIG.USE_RESET {false} \
    ] $clk_wiz_svm

    set clk_reset_svm [tapasco::ip::create_rst_gen clk_reset_svm]
    set rx_reset_inv [create_bd_cell -type ip -vlnv xilinx.com:ip:util_vector_logic:2.0 rx_reset_inverter_svm]
    set_property -dict [list CONFIG.C_SIZE {1} CONFIG.C_OPERATION {not} CONFIG.LOGO_FILE {data/sym_notgate.png}] $rx_reset_inv
    set tx_reset_inv [create_bd_cell -type ip -vlnv xilinx.com:ip:util_vector_logic:2.0 tx_reset_inverter_svm]
    set_property -dict [list CONFIG.C_SIZE {1} CONFIG.C_OPERATION {not} CONFIG.LOGO_FILE {data/sym_notgate.png}] $tx_reset_inv
    set aligned_inv [create_bd_cell -type ip -vlnv xilinx.com:ip:util_vector_logic:2.0 aligned_inverter_svm]
    set_property -dict [list CONFIG.C_SIZE {1} CONFIG.C_OPERATION {not} CONFIG.LOGO_FILE {data/sym_notgate.png}] $aligned_inv

    # create 100G core
    set eth_core [tapasco::ip::create_100g_ethernet ethernet_svm]
    svm::customize_100g_core $eth_core $mac_addr $port_no

    set flow_ctrl [tapasco::ip::create_eth_flow_ctrl flow_ctrl_0]

    # create AXI Stream interconnects and register slices
    set axis_tx_ic [tapasco::ip::create_axis_ic axis_tx_ic_0 1 1]
    set axis_rx_ic [tapasco::ip::create_axis_ic axis_rx_ic_0 1 1]
    # TODO check properties
    set_property -dict [list \
      CONFIG.ARB_ON_TLAST {1} \
      CONFIG.S00_HAS_REGSLICE {0} \
      CONFIG.S00_FIFO_DEPTH {32} \
      CONFIG.S00_FIFO_MODE {1} \
      CONFIG.M00_HAS_REGSLICE {0} \
      CONFIG.M00_FIFO_DEPTH {0} \
      CONFIG.M00_FIFO_MODE {0} \
    ] $axis_tx_ic
    set_property -dict [list \
      CONFIG.ARB_ON_TLAST {1} \
      CONFIG.S00_HAS_REGSLICE {0} \
      CONFIG.S00_FIFO_DEPTH {4096} \
      CONFIG.S00_FIFO_MODE {1} \
      CONFIG.M00_HAS_REGSLICE {0} \
      CONFIG.M00_FIFO_DEPTH {0} \
      CONFIG.M00_FIFO_MODE {0} \
    ] $axis_rx_ic

    set axis_tx_ic_outer [tapasco::ip::create_axis_ic axis_tx_ic_1 1 1]
    set axis_rx_ic_outer [tapasco::ip::create_axis_ic axis_rx_ic_1 1 1]
    # TODO check properties
    set_property -dict [list \
      CONFIG.ARB_ON_TLAST {1} \
      CONFIG.S00_HAS_REGSLICE {0} \
      CONFIG.S00_FIFO_DEPTH {0} \
      CONFIG.S00_FIFO_MODE {0} \
      CONFIG.M00_HAS_REGSLICE {0} \
      CONFIG.M00_FIFO_DEPTH {0} \
      CONFIG.M00_FIFO_MODE {0} \
    ] $axis_tx_ic_outer
    set_property -dict [list \
      CONFIG.ARB_ON_TLAST {1} \
      CONFIG.S00_HAS_REGSLICE {0} \
      CONFIG.S00_FIFO_DEPTH {512} \
      CONFIG.S00_FIFO_MODE {0} \
      CONFIG.M00_HAS_REGSLICE {0} \
      CONFIG.M00_FIFO_DEPTH {0} \
      CONFIG.M00_FIFO_MODE {0} \
    ] $axis_rx_ic_outer

    set axis_tx_rs [tapasco::ip::create_axis_reg_slice axis_tx_rs_0]
    set axis_rx_rs [tapasco::ip::create_axis_reg_slice axis_rx_rs_0]
    svm::customize_stream_regslices $axis_tx_rs $axis_rx_rs

    # create interface pins
    set axis_rx_intf [create_bd_intf_pin -mode Master  -vlnv xilinx.com:interface:axis_rtl:1.0 AXIS_NETWORK_RX]
    set axis_tx_intf [create_bd_intf_pin -mode Slave -vlnv xilinx.com:interface:axis_rtl:1.0 AXIS_NETWORK_TX]
    svm::create_custom_interfaces $eth_core $const_zero $const_one $clk_reset_svm

    # set constraints
    set constraints_fn "[get_property DIRECTORY [current_project]]/svm.xdc"
    set constraints_file [open $constraints_fn "w"]

    svm::set_custom_constraints $constraints_file $port_no

    close $constraints_file
    read_xdc $constraints_fn
    set_property PROCESSING_ORDER NORMAL [get_files $constraints_fn]

    # connect clocks and resets
    connect_bd_net $pcie_aclk [get_bd_pins $clk_wiz_svm/clk_in1]
    connect_bd_net [get_bd_pins $clk_wiz_svm/clk_out1] \
      [get_bd_pins $eth_core/init_clk] \
      [get_bd_pins $eth_core/drp_clk] \
      [get_bd_pins $clk_reset_svm/slowest_sync_clk]
    connect_bd_net $pcie_p_aresetn [get_bd_pins $clk_reset_svm/ext_reset_in]
    connect_bd_net [get_bd_pins $clk_reset_svm/peripheral_reset] [get_bd_pins $eth_core/sys_reset]

    connect_bd_net [get_bd_pins $eth_core/usr_rx_reset] [get_bd_pins $rx_reset_inv/Op1]
    connect_bd_net [get_bd_pins $eth_core/usr_tx_reset] [get_bd_pins $tx_reset_inv/Op1]
    connect_bd_net [get_bd_pins $eth_core/gt_txusrclk2] [get_bd_pins $eth_core/rx_clk]

    connect_bd_net $pcie_aclk [get_bd_pins $axis_tx_ic/ACLK] [get_bd_pins $axis_tx_ic/S00_AXIS_ACLK]
    connect_bd_net $pcie_p_aresetn [get_bd_pins $axis_tx_ic/ARESETN] [get_bd_pins $axis_tx_ic/S00_AXIS_ARESETN]
    connect_bd_net [get_bd_pins $eth_core/gt_txusrclk2] [get_bd_pins $axis_tx_ic/M00_AXIS_ACLK]
    connect_bd_net [get_bd_pins $tx_reset_inv/Res] [get_bd_pins $axis_tx_ic/M00_AXIS_ARESETN]

    connect_bd_net $pcie_aclk [get_bd_pins $axis_rx_ic/ACLK] [get_bd_pins $axis_rx_ic/M00_AXIS_ACLK]
    connect_bd_net $pcie_p_aresetn [get_bd_pins $axis_rx_ic/ARESETN] [get_bd_pins $axis_rx_ic/M00_AXIS_ARESETN]
    connect_bd_net [get_bd_pins $eth_core/gt_txusrclk2] [get_bd_pins $axis_rx_ic/S00_AXIS_ACLK]
    connect_bd_net [get_bd_pins $rx_reset_inv/Res] [get_bd_pins $axis_rx_ic/S00_AXIS_ARESETN]

    connect_bd_net [get_bd_pins $eth_core/gt_txusrclk2] [get_bd_pins $flow_ctrl/aclk]
    connect_bd_net [get_bd_pins $tx_reset_inv/Res] [get_bd_pins $flow_ctrl/aresetn]

    connect_bd_net [get_bd_pins $eth_core/gt_txusrclk2] \
      [get_bd_pins $axis_rx_ic_outer/ACLK] \
      [get_bd_pins $axis_rx_ic_outer/S00_AXIS_ACLK] \
      [get_bd_pins $axis_rx_ic_outer/M00_AXIS_ACLK]
    connect_bd_net [get_bd_pins $eth_core/gt_txusrclk2] \
      [get_bd_pins $axis_tx_ic_outer/ACLK] \
      [get_bd_pins $axis_tx_ic_outer/S00_AXIS_ACLK] \
      [get_bd_pins $axis_tx_ic_outer/M00_AXIS_ACLK]
    connect_bd_net [get_bd_pins $rx_reset_inv/Res] \
      [get_bd_pins $axis_rx_ic_outer/ARESETN] \
      [get_bd_pins $axis_rx_ic_outer/S00_AXIS_ARESETN]
    connect_bd_net [get_bd_pins $tx_reset_inv/Res] \
      [get_bd_pins $axis_tx_ic_outer/ARESETN] \
      [get_bd_pins $axis_tx_ic_outer/S00_AXIS_ARESETN] \
      [get_bd_pins $axis_tx_ic_outer/M00_AXIS_ARESETN] \
      [get_bd_pins $axis_rx_ic_outer/M00_AXIS_ARESETN]

    connect_bd_net $pcie_aclk [get_bd_pins $axis_tx_rs/aclk] [get_bd_pins $axis_rx_rs/aclk]
    connect_bd_net $pcie_p_aresetn [get_bd_pins $axis_tx_rs/aresetn] [get_bd_pins $axis_rx_rs/aresetn]

    # 100G core connections
    connect_bd_net [get_bd_pins $eth_core/stat_rx_aligned] [get_bd_pins $eth_core/ctl_tx_enable] [get_bd_pins $aligned_inv/Op1]
    connect_bd_net [get_bd_pins $aligned_inv/Res] [get_bd_pins $eth_core/ctl_tx_send_rfi]
    connect_bd_net [get_bd_pins $const_one/dout] \
      [get_bd_pins $eth_core/ctl_rx_enable] \
      [get_bd_pins $eth_core/ctl_rx_rsfec_enable] \
      [get_bd_pins $eth_core/ctl_rx_rsfec_enable_correction] \
      [get_bd_pins $eth_core/ctl_rx_rsfec_enable_indication] \
      [get_bd_pins $eth_core/ctl_tx_rsfec_enable]

    connect_bd_net [get_bd_pins $eth_core/stat_rx_pause_req] [get_bd_pins $flow_ctrl/stat_rx_pause_req]
    connect_bd_net [get_bd_pins $flow_ctrl/ctl_tx_pause_enable] [get_bd_pins $eth_core/ctl_tx_pause_enable]
    connect_bd_net [get_bd_pins $flow_ctrl/ctl_tx_pause_req] [get_bd_pins $eth_core/ctl_tx_pause_req]
    connect_bd_net [get_bd_pins $flow_ctrl/ctl_tx_pause_quanta8] [get_bd_pins $eth_core/ctl_tx_pause_quanta8]
    connect_bd_net [get_bd_pins $flow_ctrl/ctl_tx_pause_refresh_timer8] [get_bd_pins $eth_core/ctl_tx_pause_refresh_timer8]
    connect_bd_net [get_bd_pins $flow_ctrl/ctl_rx_pause_enable] [get_bd_pins $eth_core/ctl_rx_pause_enable]
    connect_bd_net [get_bd_pins $flow_ctrl/ctl_rx_pause_ack] [get_bd_pins $eth_core/ctl_rx_pause_ack]

    connect_bd_net [get_bd_pins $const_one/dout] \
      [get_bd_pins $eth_core/ctl_rx_check_etype_gcp] \
      [get_bd_pins $eth_core/ctl_rx_check_etype_gpp] \
      [get_bd_pins $eth_core/ctl_rx_check_mcast_gcp] \
      [get_bd_pins $eth_core/ctl_rx_check_mcast_gpp] \
      [get_bd_pins $eth_core/ctl_rx_check_opcode_gcp] \
      [get_bd_pins $eth_core/ctl_rx_check_opcode_gpp] \
      [get_bd_pins $eth_core/ctl_rx_enable_gcp] \
      [get_bd_pins $eth_core/ctl_rx_enable_gpp]

    # AXI connections
    connect_bd_intf_net $axis_tx_intf [get_bd_intf_pins $axis_tx_rs/S_AXIS]
    connect_bd_intf_net [get_bd_intf_pins $axis_tx_rs/M_AXIS] [get_bd_intf_pins $axis_tx_ic/S00_AXIS]
    connect_bd_intf_net [get_bd_intf_pins $axis_tx_ic/M00_AXIS] [get_bd_intf_pins $flow_ctrl/S_AXIS_TX]
    connect_bd_intf_net [get_bd_intf_pins $flow_ctrl/M_AXIS_TX] [get_bd_intf_pins $axis_tx_ic_outer/S00_AXIS]
    connect_bd_intf_net [get_bd_intf_pins $axis_tx_ic_outer/M00_AXIS] [get_bd_intf_pins $eth_core/axis_tx]
    connect_bd_intf_net [get_bd_intf_pins $eth_core/axis_rx] [get_bd_intf_pins $axis_rx_ic_outer/S00_AXIS]
    connect_bd_intf_net [get_bd_intf_pins $axis_rx_ic_outer/M00_AXIS] [get_bd_intf_pins $flow_ctrl/S_AXIS_RX]
    connect_bd_intf_net [get_bd_intf_pins $flow_ctrl/M_AXIS_RX] [get_bd_intf_pins $axis_rx_ic/S00_AXIS]
    connect_bd_intf_net [get_bd_intf_pins $axis_rx_ic/M00_AXIS] [get_bd_intf_pins $axis_rx_rs/S_AXIS]
    connect_bd_intf_net [get_bd_intf_pins $axis_rx_rs/M_AXIS] $axis_rx_intf
  }
}

namespace eval svm {

  proc is_svm_supported {} {
    return false
  }

  proc is_network_port_valid {port_no} {
    return false
  }

  proc add_iommu {} {
    if {[tapasco::is_feature_enabled "SVM"]} {

      if {![is_svm_supported]} {
        puts "ERROR: SVM not supported by specified platform"
        exit 1
      }

      # add slave port to host subsystem
      current_bd_instance "/host"
      set m_mmu [create_bd_intf_pin -mode Master -vlnv xilinx.com:interface:aximm_rtl:1.0 "M_MMU"]
      set num_mi_out_old [get_property CONFIG.NUM_MI [get_bd_cells out_ic]]
      set num_mi_out [expr "$num_mi_out_old + 1"]
      set_property -dict [list CONFIG.NUM_MI $num_mi_out] [get_bd_cells out_ic]
      connect_bd_intf_net [get_bd_intf_pins out_ic/[format "M%02d_AXI" $num_mi_out_old]] $m_mmu

      # remove BlueDMA and insert PageDMA core
      current_bd_instance "/memory"
      delete_bd_objs [get_bd_nets dma_IRQ_write] [get_bd_nets dma_IRQ_read] [get_bd_intf_nets S_DMA_1] [get_bd_intf_nets dma_m32_axi] [get_bd_intf_nets dma_m64_axi] [get_bd_cells dma]

      set pcie_aclk [tapasco::subsystem::get_port "host" "clk"]
      set pcie_p_aresetn [tapasco::subsystem::get_port "host" "rst" "peripheral" "resetn"]
      set design_aclk [tapasco::subsystem::get_port "design" "clk"]

      if {[tapasco::get_feature_option "SVM" "network_dma"] == "true"} {
        set mac_addr [tapasco::get_feature_option "SVM" "mac_addr"]
        set page_dma [tapasco::ip::create_network_page_dma dma_0]
        set_property -dict [list CONFIG.mac_addr $mac_addr] $page_dma
      } else {
        set page_dma [tapasco::ip::create_page_dma dma_0]
      }

      set mig_ic [get_bd_cells mig_ic]
      connect_bd_net $pcie_aclk [get_bd_pins $page_dma/aclk]
      connect_bd_net $pcie_p_aresetn [get_bd_pins $page_dma/resetn]
      connect_bd_net [get_bd_pins $page_dma/intr_c2h] [get_bd_pins intr_PLATFORM_COMPONENT_DMA0_READ]
      connect_bd_net [get_bd_pins $page_dma/intr_h2c] [get_bd_pins intr_PLATFORM_COMPONENT_DMA0_WRITE]
      connect_bd_intf_net [get_bd_intf_pins S_DMA] [get_bd_intf_pins $page_dma/S_AXI_CTRL]
      connect_bd_intf_net [get_bd_intf_pins $page_dma/M_AXI_MEM] [get_bd_intf_pins $mig_ic/S00_AXI]
      connect_bd_intf_net [get_bd_intf_pins $page_dma/M_AXI_PCI] [get_bd_intf_pins M_HOST]

      # add MMU to memory subsystem
      current_bd_instance "/memory"

      set s_mmu [create_bd_intf_pin -mode Slave -vlnv xilinx.com:interface:aximm_rtl:1.0 "S_MMU"]
      set mmu [tapasco::ip::create_tapasco_mmu mmu_0]
      set mmu_sc [tapasco::ip::create_axi_sc "mmu_sc" 1 1 2]

      connect_bd_net $pcie_aclk [get_bd_pins $mmu/aclk]
      connect_bd_net $pcie_p_aresetn [get_bd_pins $mmu/resetn]
      connect_bd_net $pcie_aclk [get_bd_pins $mmu_sc/aclk]
      connect_bd_net $design_aclk [get_bd_pins $mmu_sc/aclk1]

      delete_bd_objs [get_bd_intf_nets "S_MEM_0_1"]
      connect_bd_intf_net [get_bd_intf_pins S_MMU] [get_bd_intf_pins $mmu/S_AXI_CTRL]

      # FIXME put Smartconnect between target IPs and MMU to perform clock convertion for now
      # can be left out as soon as we switch from Interconnects to Smartconnects in the interconnect tree
      connect_bd_intf_net [get_bd_intf_pins S_MEM_0] [get_bd_intf_pins $mmu_sc/S00_AXI]
      connect_bd_intf_net [get_bd_intf_pins $mmu_sc/M00_AXI] [get_bd_intf_pins $mmu/S_AXI_ACC]
      connect_bd_intf_net [get_bd_intf_pins $mmu/M_AXI_MEM] [get_bd_intf_pins mig_ic/S01_AXI]

      connect_bd_net [get_bd_pins $mmu/pgf_intr] [tapasco::ip::add_interrupt "PLATFORM_COMPONENT_MMU_FAULT" "host"]

      # if page migrations over the network are enabled, create Aurora core and connect NetworkPageDMA
      # FIXME make network port configurable (see sfpplus feature as reference)
      if {[tapasco::get_feature_option "SVM" "network_dma"] == "true"} {
        set axis_rx_intf [create_bd_intf_pin -mode Slave  -vlnv xilinx.com:interface:axis_rtl:1.0 AXIS_NETWORK_RX]
        set axis_tx_intf [create_bd_intf_pin -mode Master -vlnv xilinx.com:interface:axis_rtl:1.0 AXIS_NETWORK_TX]

        connect_bd_intf_net [get_bd_intf_pins $page_dma/AXIS_NETWORK_TX] $axis_tx_intf
        connect_bd_intf_net $axis_rx_intf [get_bd_intf_pins $page_dma/AXIS_NETWORK_RX]
      }

      current_bd_instance
      save_bd_design
    }
  }

  proc connect_rdma_offset_core {rdma_offset} {
    # add slave port to mig_ic and connect it to axi_generic_offset
    set mig_ic [get_bd_cells mig_ic]
    set num_si_old [get_property CONFIG.NUM_SI $mig_ic]
    set num_si [expr "$num_si_old + 1"]
    set_property -dict [list CONFIG.NUM_SI $num_si] $mig_ic
    connect_bd_intf_net [get_bd_intf_pin $rdma_offset/M_AXI] [get_bd_intf_pin $mig_ic/[format "S%02d_AXI" $num_si_old]]
  }

  proc add_rdma_bar {} {
    if {[tapasco::is_feature_enabled "SVM"] && [tapasco::get_feature_option "SVM" "pcie_e2e"] == "true"} {
      ## host subsystem modifications
      current_bd_instance "/host"

      # activate second BAR in PCIe core
      set pcie [get_bd_cells axi_pcie3_0]
      set_property -dict [list\
        CONFIG.pf0_bar2_enabled {true}\
        CONFIG.pf0_bar2_size {128}\
        CONFIG.pf0_bar2_scale {Gigabytes}\
        CONFIG.pf0_bar2_64bit {true}\
        CONFIG.pf0_bar2_prefetchable {true}\
        CONFIG.pciebar2axibar_2 {0x0000002000000000}\
      ] $pcie

      # add master port to out_ic and connect to new external port M_RDMA
      set m_rdma [create_bd_intf_pin -mode Master -vlnv xilinx.com:interface:aximm_rtl:1.0 "M_RDMA"]
      set out_ic [get_bd_cells out_ic]
      set num_mi_out_old [get_property CONFIG.NUM_MI $out_ic]
      set num_mi_out [expr "$num_mi_out_old + 1"]
      set_property -dict [list CONFIG.NUM_MI $num_mi_out] $out_ic
      connect_bd_intf_net [get_bd_intf_pin $out_ic/[format "M%02d_AXI" $num_mi_out_old]] $m_rdma

      ## memory subsystem modifications
      current_bd_instance "/memory"
      set pcie_aclk [tapasco::subsystem::get_port "host" "clk"]
      set pcie_p_aresetn [tapasco::subsystem::get_port "host" "rst" "peripheral" "resetn"]

      # add axi_generic_offset and connect it to new external port S_RDMA
      set s_rdma [create_bd_intf_pin -mode Slave -vlnv xilinx.com:interface:aximm_rtl:1.0 "S_RDMA"]
      set rdma_offset [tapasco::ip::create_axi_generic_off "rdma_offset"]
      set_property -dict [list\
        CONFIG.BYTES_PER_WORD {64}\
        CONFIG.ADDRESS_WIDTH {38}\
        CONFIG.ID_WIDTH {1}\
      ] $rdma_offset
      connect_bd_net $pcie_aclk [get_bd_pin $rdma_offset/aclk]
      connect_bd_net $pcie_p_aresetn [get_bd_pin $rdma_offset/aresetn]
      connect_bd_intf_net $s_rdma [get_bd_intf_pin $rdma_offset/S_AXI]

      # add slave port to mig_ic and connect it to axi_generic_offset
      connect_rdma_offset_core $rdma_offset

      current_bd_instance
    }
  }

  proc customize_100g_core {eth_core mac_addr} {
    puts "ERROR: 100G Ethernet core generation not implemented for specified platform"
    exit 1
  }

  proc customize_stream_regslices {tx_rs rx_rs} {
  }

  proc set_custom_constraints {constraints_file} {
  }

  proc create_custom_interfaces {eth_core const_zero_core const_one_core clk_reset_core} {
  }

  proc addressmap {{args {}}} {
    if {[tapasco::is_feature_enabled "SVM"]} {
      set args [lappend args "M_MMU" [list 0x50000 0x10000 0 "PLATFORM_COMPONENT_MMU"]]
      if {[tapasco::get_feature_option "SVM" "pcie_e2e"] == "true"} {
        set args [lappend args "M_RDMA" [list 0x2000000000 0 [expr "1 << 37"] ""]]
      }
    }
    return $args
  }
}

tapasco::register_plugin "platform::svm::add_iommu" "pre-wiring"
tapasco::register_plugin "platform::svm::add_rdma_bar" "pre-wiring"
tapasco::register_plugin "platform::svm::addressmap" "post-address-map"

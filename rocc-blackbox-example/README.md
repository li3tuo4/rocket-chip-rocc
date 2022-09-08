# How to use
## Apply patch
* `git checkout [hash]` in `chisel3`, `firrtl`, `hardfloat` directories 
according to the hash values in the `.hash` files.

* `git clone` url in `testchipip.url` to replace the existing `testchipip` (only for FPGA flow), 
or  `git pull` in existing `testchipip` and switch to the `chisel3-adapt` branch.

* Copy files in `rocc-blackbox-example/fpga-zynq` directory to 
replace the existing files in the same locations based on 
the root-dir (called `fpga-zynq` as well).
Note, do not copy the directories directly, 
because these directories only include the files that are changed.

## Use RoCC blackbox
* In `/emulator`, do `make CONFIG=RoccExampleConfig` and 
so forth to create the RTL simulation model that uses RoCC.

* In `zcu102`, do `make CONFIG=ZynqRoccExampleConfig [target]` to use RoCC in the hardware build. 

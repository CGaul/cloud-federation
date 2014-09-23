#!/bin/bash

echo "Installing Archlinux VM..."
virt-install \
	--name archlinux \
	--description "First archlinux VM" \
	--virt-type kvm \
	--ram 1024 \
	--vcpus 2 \
	--network network=default \
	--os-variant virtio26 \
	--os-type linux \
	--disk path="/home/costa/Downloads/Betriebssysteme/ArchLinux/archlinux-2014.09.03-dual.iso",device=cdrom \
	--boot cdrom,hd,network \
	--vnc \
	--vnclisten=0.0.0.0

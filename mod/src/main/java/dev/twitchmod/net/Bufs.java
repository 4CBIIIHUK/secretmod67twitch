package dev.twitchmod.net;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

/** Thin wrapper so client code never imports the fabric helper directly. */
public final class Bufs {
    private Bufs() {
    }

    public static PacketByteBuf create() {
        return PacketByteBufs.create();
    }
}

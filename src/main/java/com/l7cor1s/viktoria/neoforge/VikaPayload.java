package com.l7cor1s.viktoria.neoforge;

import com.l7cor1s.viktoria.ViktoriaMod;
import com.l7cor1s.viktoria.network.VikaNetwork;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The single mod packet: raw bytes encoded and decoded by the common code.
 *
 * @author vyrriox
 */
public record VikaPayload(byte[] data) implements CustomPacketPayload {
    public static final Type<VikaPayload> TYPE = new Type<>(ViktoriaMod.id(VikaNetwork.CHANNEL));
    public static final StreamCodec<ByteBuf, VikaPayload> STREAM_CODEC =
            ByteBufCodecs.byteArray(VikaNetwork.MAX_PACKET).map(VikaPayload::new, VikaPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

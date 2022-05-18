package de.ambertation.wunderreich.mixin;

import de.ambertation.wunderreich.config.LevelData;
import de.ambertation.wunderreich.interfaces.WunderKisteExtensionProvider;
import de.ambertation.wunderreich.utils.WunderKisteServerExtension;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;

import com.mojang.datafixers.DataFixer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.Proxy;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements WunderKisteExtensionProvider {
    private final WunderKisteServerExtension wunderkiste = new WunderKisteServerExtension();

    public WunderKisteServerExtension getWunderKisteExtension() {
        return wunderkiste;
    }

    @Inject(method = "stopServer", at = @At("HEAD"))
    public void wunderreich_stop(CallbackInfo ci) {
        wunderkiste.onCloseServer();
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    public void wunderreich_init(Thread thread,
                                 LevelStorageAccess levelStorageAccess,
                                 PackRepository packRepository,
                                 WorldStem worldStem,
                                 Proxy proxy,
                                 DataFixer dataFixer,
                                 Services services,
                                 ChunkProgressListenerFactory chunkProgressListenerFactory,
                                 CallbackInfo ci) {
        LevelData.getInstance().loadNewLevel(levelStorageAccess);
        wunderkiste.onStartServer();
    }

}

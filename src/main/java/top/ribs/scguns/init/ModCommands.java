package top.ribs.scguns.init;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import top.ribs.scguns.config.RaidConfig;
import top.ribs.scguns.entity.player.GunTier;
import top.ribs.scguns.entity.player.GunTierRegistry;
import top.ribs.scguns.entity.player.PlayerGunProgression;
import top.ribs.scguns.entity.raid.ActiveRaid;
import top.ribs.scguns.entity.raid.RaidManager;
import top.ribs.scguns.event.GunProgressionEventHandler;

public class ModCommands {
   public ModCommands() {
      super();
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("scguns")
               .then(
                  ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("progression")
                           .then(
                              Commands.literal("set")
                                 .then(
                                    Commands.argument("player", EntityArgument.player())
                                       .then(Commands.argument("tier", StringArgumentType.string()).suggests((context, builder) -> {
                                          for (GunTier tier : GunTierRegistry.getAllTiers()) {
                                             builder.suggest(tier.getId());
                                          }

                                          return builder.buildFuture();
                                       }).executes(context -> {
                                          ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                          String tierName = StringArgumentType.getString(context, "tier");
                                          return executeSetProgression((CommandSourceStack)context.getSource(), player, tierName);
                                       }))
                                 )
                           ))
                        .then(Commands.literal("clear").then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                           ServerPlayer player = EntityArgument.getPlayer(context, "player");
                           return executeClearProgression((CommandSourceStack)context.getSource(), player);
                        }))))
                     .then(Commands.literal("check")
                        // Self-check first: any player may ask about their own progression, which is
                        // what makes the tier system legible in game. Checking someone else stays at
                        // permission level 2.
                        .executes(context -> executeCheckProgression((CommandSourceStack)context.getSource(), null))
                        .then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                           ServerPlayer player = EntityArgument.getPlayer(context, "player");
                           return executeCheckProgression((CommandSourceStack)context.getSource(), player);
                        })))
                     .then(Commands.literal("info").then(Commands.argument("tier", StringArgumentType.string()).suggests((context, builder) -> {
                        for (GunTier tier : GunTierRegistry.getAllTiers()) {
                           builder.suggest(tier.getId());
                        }

                        return builder.buildFuture();
                     }).executes(context -> executeInfoTier((CommandSourceStack)context.getSource(), StringArgumentType.getString(context, "tier")))))
               ))
            .then(
               ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("raid")
                              .then(Commands.literal("start").then(Commands.argument("raid_id", StringArgumentType.string()).suggests((context, builder) -> {
                                 for (RaidConfig.RaidData raid : RaidConfig.getAllRaids()) {
                                    builder.suggest(raid.raidId());
                                 }

                                 return builder.buildFuture();
                              }).executes(context -> {
                                 String raidId = StringArgumentType.getString(context, "raid_id");
                                 return executeStartRaidById((CommandSourceStack)context.getSource(), raidId);
                              }))))
                           .then(Commands.literal("stop").executes(context -> executeStopAllRaids((CommandSourceStack)context.getSource()))))
                        .then(Commands.literal("list").executes(context -> executeListRaids((CommandSourceStack)context.getSource()))))
                     .then(Commands.literal("listall").executes(context -> executeListAllAvailableRaids((CommandSourceStack)context.getSource()))))
                  .then(Commands.literal("startnext").executes(context -> executeStartNextRaid((CommandSourceStack)context.getSource())))
            )
      );
   }

   private static int executeStartRaidById(CommandSourceStack source, String raidId) {
      if (!source.hasPermission(2)) {
         source.sendFailure(Component.translatable("commands.scguns.no_permission"));
         return 0;
      } else {
         ServerLevel serverLevel = source.getLevel();
         RaidConfig.RaidData raidConfig = RaidConfig.getRaidById(raidId);
         if (raidConfig == null) {
            source.sendFailure(Component.translatable("commands.scguns.raid.no_config", new Object[]{raidId}));
            return 0;
         } else {
            Vec3 sourcePos = source.getPosition();
            RaidManager manager = RaidManager.get(serverLevel);
            manager.startRaid(raidConfig, serverLevel, sourcePos);
            Component raidName = Component.literal(raidConfig.raidId()).withStyle(ChatFormatting.GOLD);
            source.sendSuccess(() -> Component.translatable("commands.scguns.raid.started", new Object[]{raidName}), true);
            return 1;
         }
      }
   }

   private static int executeListAllAvailableRaids(CommandSourceStack source) {
      if (!source.hasPermission(2)) {
         source.sendFailure(Component.translatable("commands.scguns.no_permission"));
         return 0;
      } else {
         Collection<RaidConfig.RaidData> progressionRaids = RaidConfig.getProgressionRaids();
         Collection<RaidConfig.RaidData> customRaids = RaidConfig.getCustomRaids();
         source.sendSuccess(() -> Component.literal("=== Available Raids ===").withStyle(ChatFormatting.GOLD), false);
         if (!progressionRaids.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Progression Raids:").withStyle(ChatFormatting.YELLOW), false);

            for (RaidConfig.RaidData raid : progressionRaids) {
               String levelStr = raid.raidLevel() != null ? "Level " + raid.raidLevel() : "NONE";
               source.sendSuccess(() -> Component.literal("  - " + raid.raidId() + " (" + levelStr + ")").withStyle(ChatFormatting.WHITE), false);
            }
         }

         if (!customRaids.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Custom Raids:").withStyle(ChatFormatting.AQUA), false);

            for (RaidConfig.RaidData raid : customRaids) {
               source.sendSuccess(() -> Component.literal("  - " + raid.raidId()).withStyle(ChatFormatting.WHITE), false);
            }
         }

         if (progressionRaids.isEmpty() && customRaids.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No raids configured!").withStyle(ChatFormatting.RED), false);
         }

         return 1;
      }
   }

   private static int executeStartNextRaid(CommandSourceStack source) {
      if (!source.hasPermission(2)) {
         source.sendFailure(Component.translatable("commands.scguns.no_permission"));
         return 0;
      } else if (source.getEntity() instanceof ServerPlayer player) {
         ServerLevel var10 = source.getLevel();
         PlayerGunProgression progression = PlayerGunProgression.get(player);
         int currentRaidLevel = progression.getCurrentRaidLevel();
         if (currentRaidLevel == 0) {
            source.sendFailure(Component.translatable("commands.scguns.unlock_gun"));
            return 0;
         } else {
            List<RaidConfig.RaidData> availableRaids = RaidConfig.getRaidsForLevel(currentRaidLevel);
            if (availableRaids.isEmpty()) {
               source.sendFailure(Component.translatable("commands.scguns.raid.none_for_level", new Object[]{currentRaidLevel}));
               return 0;
            } else {
               RaidConfig.RaidData selectedRaid = availableRaids.get(var10.getRandom().nextInt(availableRaids.size()));
               Vec3 sourcePos = source.getPosition();
               RaidManager manager = RaidManager.get(var10);
               manager.startRaid(selectedRaid, var10, sourcePos);
               Component raidName = Component.literal(selectedRaid.raidId()).withStyle(ChatFormatting.GOLD);
               source.sendSuccess(() -> Component.translatable("commands.scguns.raid.started", new Object[]{raidName}), true);
               return 1;
            }
         }
      } else {
         source.sendFailure(Component.translatable("commands.scguns.requires_player"));
         return 0;
      }
   }

   private static int executeSetProgression(CommandSourceStack source, ServerPlayer player, String tierName) {
      if (!source.hasPermission(2)) {
         source.sendFailure(Component.translatable("commands.scguns.no_permission"));
         return 0;
      } else {
         GunTier tier = GunTierRegistry.getTier(tierName.toLowerCase());
         if (tier == null) {
            source.sendFailure(Component.translatable("commands.scguns.progression.invalid_tier", new Object[]{tierName}));
            source.sendFailure(Component.literal("Available tiers: ").withStyle(ChatFormatting.GRAY));
            StringBuilder tiersList = new StringBuilder();

            for (GunTier availableTier : GunTierRegistry.getAllTiers()) {
               if (tiersList.length() > 0) {
                  tiersList.append(", ");
               }

               tiersList.append(availableTier.getId());
            }

            source.sendFailure(Component.literal(tiersList.toString()).withStyle(ChatFormatting.YELLOW));
            return 0;
         } else {
            PlayerGunProgression progression = PlayerGunProgression.get(player);
            progression.setTier(tier);
            PlayerGunProgression.save(player, progression);
            GunProgressionEventHandler.sendTierUnlockedMessage(player, tier);
            Component tierComponent = Component.translatable("gun_tier.scguns." + tier.getId()).withStyle(ChatFormatting.GOLD);
            source.sendSuccess(() -> Component.translatable("commands.scguns.progression.set", new Object[]{player.getDisplayName(), tierComponent}), true);
            return 1;
         }
      }
   }

   private static int executeClearProgression(CommandSourceStack source, ServerPlayer player) {
      if (!source.hasPermission(2)) {
         source.sendFailure(Component.translatable("commands.scguns.no_permission"));
         return 0;
      } else {
         PlayerGunProgression progression = new PlayerGunProgression();
         PlayerGunProgression.save(player, progression);
         player.sendSystemMessage(Component.translatable("commands.scguns.progression.reset").withStyle(ChatFormatting.RED));
         source.sendSuccess(() -> Component.translatable("commands.scguns.progression.cleared", new Object[]{player.getDisplayName()}), true);
         return 1;
      }
   }

   /**
    * Report a player's gun progression.
    *
    * <p>{@code player} may be null, meaning "whoever ran the command": that path needs no permission,
    * because until now the only way to see a tier was an operator running {@code check <player>}, and
    * the output was English-only literals. The labels are translated and the enemy/raid lists are the
    * same information the tier-unlock notice announces.</p>
    */
   private static int executeCheckProgression(CommandSourceStack source, ServerPlayer player) {
      ServerPlayer target;
      if (player == null) {
         try {
            target = source.getPlayerOrException();
         } catch (Exception e) {
            source.sendFailure(Component.translatable("commands.scguns.requires_player"));
            return 0;
         }
      } else if (!source.hasPermission(2)) {
         source.sendFailure(Component.translatable("commands.scguns.no_permission"));
         return 0;
      } else {
         target = player;
      }

      PlayerGunProgression progression = PlayerGunProgression.get(target);
      GunTier currentTier = progression.getCurrentTier();
      int raidLevel = progression.getCurrentRaidLevel();
      Component tierComponent = Component.translatable("gun_tier.scguns." + currentTier.getId()).withStyle(ChatFormatting.GOLD);

      source.sendSuccess(() -> Component.translatable("commands.scguns.progression.check.header",
         new Object[]{target.getDisplayName()}).withStyle(ChatFormatting.YELLOW), false);
      source.sendSuccess(() -> Component.translatable("commands.scguns.progression.check.tier",
         new Object[]{tierComponent, currentTier.getLevel()}).withStyle(ChatFormatting.GRAY), false);
      source.sendSuccess(() -> Component.translatable("commands.scguns.progression.check.raid_level",
         new Object[]{raidLevel}).withStyle(ChatFormatting.AQUA), false);
      source.sendSuccess(() -> Component.translatable("commands.scguns.progression.check.mob_tiers",
         new Object[]{joinTierNames(currentTier.getAvailableMobTiers())}).withStyle(ChatFormatting.RED), false);
      source.sendSuccess(() -> Component.translatable("commands.scguns.progression.check.raids",
         new Object[]{joinRaidNames(RaidConfig.getRaidsForLevel(raidLevel))}).withStyle(ChatFormatting.DARK_RED), false);
      return 1;
   }

   /** What a tier brings with it: answers "what will this spawn?" without needing a player. */
   private static int executeInfoTier(CommandSourceStack source, String tierName) {
      GunTier tier = GunTierRegistry.getTier(tierName);
      if (tier == null) {
         source.sendFailure(Component.translatable("commands.scguns.progression.invalid_tier", new Object[]{tierName}));
         return 0;
      }

      Component tierComponent = Component.translatable("gun_tier.scguns." + tier.getId()).withStyle(ChatFormatting.GOLD);
      source.sendSuccess(() -> Component.translatable("commands.scguns.progression.info.header",
         new Object[]{tierComponent, tier.getLevel()}).withStyle(ChatFormatting.YELLOW), false);
      source.sendSuccess(() -> Component.translatable("commands.scguns.progression.check.mob_tiers",
         new Object[]{joinTierNames(tier.getAvailableMobTiers())}).withStyle(ChatFormatting.RED), false);
      source.sendSuccess(() -> Component.translatable("commands.scguns.progression.check.raids",
         new Object[]{joinRaidNames(RaidConfig.getRaidsForLevel(tier.getRaidLevel()))}).withStyle(ChatFormatting.DARK_RED), false);
      return 1;
   }

   /** Tier names joined with a separator each language spells its own way (、in Chinese). */
   private static Component joinTierNames(List<GunTier> tiers) {
      if (tiers.isEmpty()) {
         return Component.translatable("commands.scguns.progression.none").withStyle(ChatFormatting.GRAY);
      }

      MutableComponent joined = Component.empty();
      for (int i = 0; i < tiers.size(); i++) {
         joined.append(Component.translatable("gun_tier.scguns." + tiers.get(i).getId()).withStyle(ChatFormatting.GOLD));
         if (i < tiers.size() - 1) {
            joined.append(Component.translatable("progression.scguns.list_separator").withStyle(ChatFormatting.RED));
         }
      }

      return joined;
   }

   /** Raid display names, so the player reads a name rather than the config id. */
   private static Component joinRaidNames(List<RaidConfig.RaidData> raids) {
      if (raids.isEmpty()) {
         return Component.translatable("commands.scguns.progression.none").withStyle(ChatFormatting.GRAY);
      }

      MutableComponent joined = Component.empty();
      for (int i = 0; i < raids.size(); i++) {
         joined.append(Component.translatable("raid.scguns." + raids.get(i).raidId()).withStyle(ChatFormatting.DARK_RED));
         if (i < raids.size() - 1) {
            joined.append(Component.translatable("progression.scguns.list_separator").withStyle(ChatFormatting.GRAY));
         }
      }

      return joined;
   }

   private static int executeStopAllRaids(CommandSourceStack source) {
      if (!source.hasPermission(2)) {
         source.sendFailure(Component.translatable("commands.scguns.no_permission"));
         return 0;
      } else {
         ServerLevel serverLevel = source.getLevel();
         RaidManager manager = RaidManager.get(serverLevel);
         Collection<ActiveRaid> activeRaids = manager.getActiveRaids();
         if (activeRaids.isEmpty()) {
            source.sendFailure(Component.translatable("commands.scguns.raid.none_active"));
            return 0;
         } else {
            int count = 0;

            for (ActiveRaid raid : new ArrayList<>(activeRaids)) {
               raid.endRaid(false);
               count++;
            }

            int finalCount = count;
            source.sendSuccess(() -> Component.translatable("commands.scguns.raid.stopped", new Object[]{finalCount}), true);
            return 1;
         }
      }
   }

   private static int executeListRaids(CommandSourceStack source) {
      if (!source.hasPermission(2)) {
         source.sendFailure(Component.translatable("commands.scguns.no_permission"));
         return 0;
      } else {
         ServerLevel serverLevel = source.getLevel();
         RaidManager manager = RaidManager.get(serverLevel);
         Collection<ActiveRaid> activeRaids = manager.getActiveRaids();
         if (activeRaids.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("commands.scguns.raid.list_none"), false);
            return 1;
         } else {
            source.sendSuccess(() -> Component.translatable("commands.scguns.raid.list_header").withStyle(ChatFormatting.GOLD), false);

            for (ActiveRaid raid : activeRaids) {
               Integer raidLevel = raid.getRaidLevel();
               Component raidInfo;
               if (raidLevel != null) {
                  raidInfo = Component.literal(raid.getConfig().raidId() + " (Level " + raidLevel + ")").withStyle(ChatFormatting.YELLOW);
               } else {
                  raidInfo = Component.literal(raid.getConfig().raidId() + " (Custom)").withStyle(ChatFormatting.AQUA);
               }

               int henchmenCount = raid.getAliveHenchmenCount();
               long duration = raid.getRaidDuration() / 20L;
               Component finalRaidInfo = raidInfo;
               source.sendSuccess(() -> Component.translatable("commands.scguns.raid.list_entry", new Object[]{finalRaidInfo, henchmenCount, duration}), false);
            }

            return 1;
         }
      }
   }
}

package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.server.players.PlayerList;

public class SayCommand {
   public static void register(CommandDispatcher<CommandSourceStack> p_138410_) {
      p_138410_.register(Commands.literal("say").requires((p_138414_) -> {
         return p_138414_.hasPermission(2);
      }).then(Commands.argument("message", MessageArgument.message()).executes((p_214721_) -> {
         MessageArgument.ChatMessage messageargument$chatmessage = MessageArgument.getChatMessage(p_214721_, "message");
         CommandSourceStack commandsourcestack = p_214721_.getSource();
         PlayerList playerlist = commandsourcestack.getServer().getPlayerList();
         messageargument$chatmessage.m_232174_(commandsourcestack).thenAcceptAsync((p_214719_) -> {
            playerlist.m_215615_(p_214719_, commandsourcestack, ChatType.SAY_COMMAND);
         }, commandsourcestack.getServer());
         return 1;
      })));
   }
}
package net.minecraft.commands.arguments;

import net.minecraft.network.chat.Component;

public interface SignedArgument<T> extends PreviewedArgument<T> {
   Component getSignableText(T p_233887_);
}
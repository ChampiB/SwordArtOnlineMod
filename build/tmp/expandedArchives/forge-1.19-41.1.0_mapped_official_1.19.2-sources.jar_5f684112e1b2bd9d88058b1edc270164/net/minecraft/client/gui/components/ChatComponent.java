package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.util.Deque;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ChatComponent extends GuiComponent {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_CHAT_HISTORY = 100;
   private final Minecraft minecraft;
   private final List<String> recentChat = Lists.newArrayList();
   private final List<GuiMessage<Component>> allMessages = Lists.newArrayList();
   private final List<GuiMessage<FormattedCharSequence>> trimmedMessages = Lists.newArrayList();
   private final Deque<Component> f_93762_ = Queues.newArrayDeque();
   private int chatScrollbarPos;
   private boolean newMessageSinceScroll;
   private long f_93765_;

   public ChatComponent(Minecraft p_93768_) {
      this.minecraft = p_93768_;
   }

   public void render(PoseStack p_93781_, int p_93782_) {
      if (!this.isChatHidden()) {
         this.m_93820_();
         int i = this.getLinesPerPage();
         int j = this.trimmedMessages.size();
         if (j > 0) {
            boolean flag = this.isChatFocused();
            float f = (float)this.getScale();
            int k = Mth.ceil((float)this.getWidth() / f);
            p_93781_.pushPose();
            p_93781_.translate(4.0D, 8.0D, 0.0D);
            p_93781_.scale(f, f, 1.0F);
            double d0 = this.minecraft.options.chatOpacity().get() * (double)0.9F + (double)0.1F;
            double d1 = this.minecraft.options.textBackgroundOpacity().get();
            double d2 = this.minecraft.options.chatLineSpacing().get();
            double d3 = 9.0D * (d2 + 1.0D);
            double d4 = -8.0D * (d2 + 1.0D) + 4.0D * d2;
            int l = 0;

            for(int i1 = 0; i1 + this.chatScrollbarPos < this.trimmedMessages.size() && i1 < i; ++i1) {
               GuiMessage<FormattedCharSequence> guimessage = this.trimmedMessages.get(i1 + this.chatScrollbarPos);
               if (guimessage != null) {
                  int j1 = p_93782_ - guimessage.m_90794_();
                  if (j1 < 200 || flag) {
                     double d5 = flag ? 1.0D : getTimeFactor(j1);
                     int l1 = (int)(255.0D * d5 * d0);
                     int i2 = (int)(255.0D * d5 * d1);
                     ++l;
                     if (l1 > 3) {
                        int j2 = 0;
                        double d6 = (double)(-i1) * d3;
                        p_93781_.pushPose();
                        p_93781_.translate(0.0D, 0.0D, 50.0D);
                        fill(p_93781_, -4, (int)(d6 - d3), 0 + k + 4, (int)d6, i2 << 24);
                        RenderSystem.enableBlend();
                        p_93781_.translate(0.0D, 0.0D, 50.0D);
                        this.minecraft.font.drawShadow(p_93781_, guimessage.m_90793_(), 0.0F, (float)((int)(d6 + d4)), 16777215 + (l1 << 24));
                        RenderSystem.disableBlend();
                        p_93781_.popPose();
                     }
                  }
               }
            }

            if (!this.f_93762_.isEmpty()) {
               int k2 = (int)(128.0D * d0);
               int i3 = (int)(255.0D * d1);
               p_93781_.pushPose();
               p_93781_.translate(0.0D, 0.0D, 50.0D);
               fill(p_93781_, -2, 0, k + 4, 9, i3 << 24);
               RenderSystem.enableBlend();
               p_93781_.translate(0.0D, 0.0D, 50.0D);
               this.minecraft.font.drawShadow(p_93781_, Component.translatable("chat.queue", this.f_93762_.size()), 0.0F, 1.0F, 16777215 + (k2 << 24));
               p_93781_.popPose();
               RenderSystem.disableBlend();
            }

            if (flag) {
               int l2 = 9;
               int j3 = j * l2;
               int k3 = l * l2;
               int l3 = this.chatScrollbarPos * k3 / j;
               int k1 = k3 * k3 / j3;
               if (j3 != k3) {
                  int i4 = l3 > 0 ? 170 : 96;
                  int j4 = this.newMessageSinceScroll ? 13382451 : 3355562;
                  p_93781_.translate(-4.0D, 0.0D, 0.0D);
                  fill(p_93781_, 0, -l3, 2, -l3 - k1, j4 + (i4 << 24));
                  fill(p_93781_, 2, -l3, 1, -l3 - k1, 13421772 + (i4 << 24));
               }
            }

            p_93781_.popPose();
         }
      }
   }

   private boolean isChatHidden() {
      return this.minecraft.options.chatVisibility().get() == ChatVisiblity.HIDDEN;
   }

   private static double getTimeFactor(int p_93776_) {
      double d0 = (double)p_93776_ / 200.0D;
      d0 = 1.0D - d0;
      d0 *= 10.0D;
      d0 = Mth.clamp(d0, 0.0D, 1.0D);
      return d0 * d0;
   }

   public void clearMessages(boolean p_93796_) {
      this.f_93762_.clear();
      this.trimmedMessages.clear();
      this.allMessages.clear();
      if (p_93796_) {
         this.recentChat.clear();
      }

   }

   public void addMessage(Component p_93786_) {
      this.m_93787_(p_93786_, 0);
   }

   private void m_93787_(Component p_93788_, int p_93789_) {
      this.m_93790_(p_93788_, p_93789_, this.minecraft.gui.getGuiTicks(), false);
      LOGGER.info("[CHAT] {}", (Object)p_93788_.getString().replaceAll("\r", "\\\\r").replaceAll("\n", "\\\\n"));
   }

   private void m_93790_(Component p_93791_, int p_93792_, int p_93793_, boolean p_93794_) {
      if (p_93792_ != 0) {
         this.m_93803_(p_93792_);
      }

      int i = Mth.floor((double)this.getWidth() / this.getScale());
      List<FormattedCharSequence> list = ComponentRenderUtils.wrapComponents(p_93791_, i, this.minecraft.font);
      boolean flag = this.isChatFocused();

      for(FormattedCharSequence formattedcharsequence : list) {
         if (flag && this.chatScrollbarPos > 0) {
            this.newMessageSinceScroll = true;
            this.scrollChat(1);
         }

         this.trimmedMessages.add(0, new GuiMessage<>(p_93793_, formattedcharsequence, p_93792_));
      }

      while(this.trimmedMessages.size() > 100) {
         this.trimmedMessages.remove(this.trimmedMessages.size() - 1);
      }

      if (!p_93794_) {
         this.allMessages.add(0, new GuiMessage<>(p_93793_, p_93791_, p_93792_));

         while(this.allMessages.size() > 100) {
            this.allMessages.remove(this.allMessages.size() - 1);
         }
      }

   }

   public void rescaleChat() {
      this.trimmedMessages.clear();
      this.resetChatScroll();

      for(int i = this.allMessages.size() - 1; i >= 0; --i) {
         GuiMessage<Component> guimessage = this.allMessages.get(i);
         this.m_93790_(guimessage.m_90793_(), guimessage.m_90795_(), guimessage.m_90794_(), true);
      }

   }

   public List<String> getRecentChat() {
      return this.recentChat;
   }

   public void addRecentChat(String p_93784_) {
      if (this.recentChat.isEmpty() || !this.recentChat.get(this.recentChat.size() - 1).equals(p_93784_)) {
         this.recentChat.add(p_93784_);
      }

   }

   public void resetChatScroll() {
      this.chatScrollbarPos = 0;
      this.newMessageSinceScroll = false;
   }

   public void scrollChat(int p_205361_) {
      this.chatScrollbarPos += p_205361_;
      int i = this.trimmedMessages.size();
      if (this.chatScrollbarPos > i - this.getLinesPerPage()) {
         this.chatScrollbarPos = i - this.getLinesPerPage();
      }

      if (this.chatScrollbarPos <= 0) {
         this.chatScrollbarPos = 0;
         this.newMessageSinceScroll = false;
      }

   }

   public boolean handleChatQueueClicked(double p_93773_, double p_93774_) {
      if (this.isChatFocused() && !this.minecraft.options.hideGui && !this.isChatHidden() && !this.f_93762_.isEmpty()) {
         double d0 = p_93773_ - 2.0D;
         double d1 = (double)this.minecraft.getWindow().getGuiScaledHeight() - p_93774_ - 40.0D;
         if (d0 <= (double)Mth.floor((double)this.getWidth() / this.getScale()) && d1 < 0.0D && d1 > (double)Mth.floor(-9.0D * this.getScale())) {
            this.addMessage(this.f_93762_.remove());
            this.f_93765_ = System.currentTimeMillis();
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Nullable
   public Style getClickedComponentStyleAt(double p_93801_, double p_93802_) {
      if (this.isChatFocused() && !this.minecraft.options.hideGui && !this.isChatHidden()) {
         double d0 = p_93801_ - 2.0D;
         double d1 = (double)this.minecraft.getWindow().getGuiScaledHeight() - p_93802_ - 40.0D;
         d0 = (double)Mth.floor(d0 / this.getScale());
         d1 = (double)Mth.floor(d1 / (this.getScale() * (this.minecraft.options.chatLineSpacing().get() + 1.0D)));
         if (!(d0 < 0.0D) && !(d1 < 0.0D)) {
            int i = Math.min(this.getLinesPerPage(), this.trimmedMessages.size());
            if (d0 <= (double)Mth.floor((double)this.getWidth() / this.getScale()) && d1 < (double)(9 * i + i)) {
               int j = (int)(d1 / 9.0D + (double)this.chatScrollbarPos);
               if (j >= 0 && j < this.trimmedMessages.size()) {
                  GuiMessage<FormattedCharSequence> guimessage = this.trimmedMessages.get(j);
                  return this.minecraft.font.getSplitter().componentStyleAtWidth(guimessage.m_90793_(), (int)d0);
               }
            }

            return null;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   @Nullable
   public ChatScreen getFocusedChat() {
      Screen screen = this.minecraft.screen;
      return screen instanceof ChatScreen ? (ChatScreen)screen : null;
   }

   private boolean isChatFocused() {
      return this.getFocusedChat() != null;
   }

   private void m_93803_(int p_93804_) {
      this.trimmedMessages.removeIf((p_93807_) -> {
         return p_93807_.m_90795_() == p_93804_;
      });
      this.allMessages.removeIf((p_93779_) -> {
         return p_93779_.m_90795_() == p_93804_;
      });
   }

   public int getWidth() {
      return getWidth(this.minecraft.options.chatWidth().get());
   }

   public int getHeight() {
      return getHeight(this.isChatFocused() ? this.minecraft.options.chatHeightFocused().get() : this.minecraft.options.chatHeightUnfocused().get() / (this.minecraft.options.chatLineSpacing().get() + 1.0D));
   }

   public double getScale() {
      return this.minecraft.options.chatScale().get();
   }

   public static int getWidth(double p_93799_) {
      int i = 320;
      int j = 40;
      return Mth.floor(p_93799_ * 280.0D + 40.0D);
   }

   public static int getHeight(double p_93812_) {
      int i = 180;
      int j = 20;
      return Mth.floor(p_93812_ * 160.0D + 20.0D);
   }

   public static double defaultUnfocusedPct() {
      int i = 180;
      int j = 20;
      return 70.0D / (double)(getHeight(1.0D) - 20);
   }

   public int getLinesPerPage() {
      return this.getHeight() / 9;
   }

   private long m_93819_() {
      return (long)(this.minecraft.options.chatDelay().get() * 1000.0D);
   }

   private void m_93820_() {
      if (!this.f_93762_.isEmpty()) {
         long i = System.currentTimeMillis();
         if (i - this.f_93765_ >= this.m_93819_()) {
            this.addMessage(this.f_93762_.remove());
            this.f_93765_ = i;
         }

      }
   }

   public void m_93808_(Component p_93809_) {
      if (this.minecraft.options.chatDelay().get() <= 0.0D) {
         this.addMessage(p_93809_);
      } else {
         long i = System.currentTimeMillis();
         if (i - this.f_93765_ >= this.m_93819_()) {
            this.addMessage(p_93809_);
            this.f_93765_ = i;
         } else {
            this.f_93762_.add(p_93809_);
         }
      }

   }
}
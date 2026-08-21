package me.cortex.voxy.client.core.model;

import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import java.util.Arrays;
import me.cortex.voxy.common.util.MemoryBuffer;
import org.lwjgl.system.MemoryUtil;

public class MipGen {
   private static final int TINT_MASK_ALPHA_BIT = 1;
   private static final ThreadLocal<MipGen.Cache> CACHE = ThreadLocal.withInitial(MipGen.Cache::new);

   private static long getOffset(int bx, int by, int i) {
      bx += i & 15;
      by += i / 16;
      return bx + by * 16 * 3;
   }

   private static void solidify(long baseAddr, byte msk, short[] SCRATCH, ByteArrayFIFOQueue QUEUE) {
      for (int idx = 0; idx < 6; idx++) {
         if ((msk >> idx & 1) != 0) {
            int bx = (idx >> 1) * 16;
            int by = (idx & 1) * 16;
            long cAddr = baseAddr + (bx + by * 16 * 3) * 4L;
            Arrays.fill(SCRATCH, (short)-1);

            for (int y = 0; y < 16; y++) {
               for (int x = 0; x < 16; x++) {
                  int colour = MemoryUtil.memGetInt(cAddr + (x + y * 16 * 3) * 4);
                  if ((colour & 0xFF000000) != 0) {
                     int pos = x + y * 16;
                     SCRATCH[pos] = (short)pos;
                     QUEUE.enqueue((byte)pos);
                  }
               }
            }

            while (!QUEUE.isEmpty()) {
               int pos = Byte.toUnsignedInt(QUEUE.dequeueByte());
               int xx = pos & 15;
               int y = pos / 16;
               short newVal = (short)(SCRATCH[pos] + 256);

               for (int D = 3; D != -1; D--) {
                  int d = 2 * (D & 1) - 1;
                  int x2 = xx + ((D & 2) == 2 ? d : 0);
                  int y2 = y + ((D & 2) == 0 ? d : 0);
                  if (x2 >= 0 && x2 < 16 && y2 >= 0 && y2 < 16) {
                     int pos2 = x2 + y2 * 16;
                     if ((newVal & '\uff00') < (SCRATCH[pos2] & '\uff00')) {
                        SCRATCH[pos2] = newVal;
                        QUEUE.enqueue((byte)pos2);
                     }
                  }
               }
            }

            for (int i = 0; i < 256; i++) {
               int d = Short.toUnsignedInt(SCRATCH[i]);
               if ((d & 0xFF00) != 0) {
                  int c = MemoryUtil.memGetInt(baseAddr + getOffset(bx, by, d & 0xFF) * 4L) & 16777215;
                  MemoryUtil.memPutInt(baseAddr + getOffset(bx, by, i) * 4L, c);
               }
            }
         }
      }
   }

   private static int encodeTintMask(int colour, int depth) {
      int alpha = colour >>> 24;
      if (alpha == 0) return colour;
      alpha = alpha & ~TINT_MASK_ALPHA_BIT | depth >>> 7 & TINT_MASK_ALPHA_BIT;
      return colour & 16777215 | alpha << 24;
   }

   private static int clearTintMask(int colour) {
      int alpha = colour >>> 24 & ~TINT_MASK_ALPHA_BIT;
      return colour & 16777215 | alpha << 24;
   }

   public static void putTextures(boolean darkened, ColourDepthTextureData[] textures, MemoryBuffer into) {
      long addr = into.address;
      int LENGTH_B = 48;
      byte solidMsk = 0;

      for (int i = 0; i < 6; i++) {
         int x = (i >> 1) * 16;
         int y = (i & 1) * 16;
         int j = 0;
         boolean anyTransparent = false;

         int[] colourData = textures[i].colour();
         int[] depthData = textures[i].depth();
         for (int t : colourData) {
            int o = ((y + (j >> ModelFactory.LAYERS)) * 48 + (j & 15) + x) * 4;
            j++;
            MemoryUtil.memPutInt(addr + o, encodeTintMask(t, depthData[j - 1]));
            anyTransparent |= (t & 0xFF000000) == 0;
         }

         solidMsk = (byte)(solidMsk | (anyTransparent ? 1 : 0) << i);
      }

      if (!darkened) {
         MipGen.Cache cache = CACHE.get();
         solidify(addr, solidMsk, cache.SCRATCH, cache.QUEUE);
      }

      long dAddr = addr;

      for (int i = 0; i < ModelFactory.LAYERS - 1; i++) {
         long sAddr = dAddr;
         dAddr += 6144 >> (i << 1);
         int sTileSize = 16 >> i;
         int dTileSize = sTileSize >> 1;
         int sWidth = sTileSize * 3;
         int dWidth = dTileSize * 3;
         for (int face = 0; face < 6; face++) {
            int sBx = (face >> 1) * sTileSize;
            int sBy = (face & 1) * sTileSize;
            int dBx = (face >> 1) * dTileSize;
            int dBy = (face & 1) * dTileSize;
            for (int px = 0; px < dTileSize; px++) {
               for (int py = 0; py < dTileSize; py++) {
                  long bp = sAddr + ((sBx + px * 2L) + (sBy + py * 2L) * sWidth) * 4;
                  int C00 = MemoryUtil.memGetInt(bp);
                  int C01 = MemoryUtil.memGetInt(bp + sWidth * 4L);
                  int C10 = MemoryUtil.memGetInt(bp + 4L);
                  int C11 = MemoryUtil.memGetInt(bp + sWidth * 4L + 4L);
                  if (i == 0) {
                     C00 = clearTintMask(C00);
                     C01 = clearTintMask(C01);
                     C10 = clearTintMask(C10);
                     C11 = clearTintMask(C11);
                  }
                  MemoryUtil.memPutInt(dAddr + ((dBx + px) + (dBy + py) * (long)dWidth) * 4L,
                     TextureUtils.mipColours(darkened, C00, C01, C10, C11));
               }
            }
         }
      }
   }

   public static void generateMipmaps(long[] textures, int size) {
   }

   private record Cache(short[] SCRATCH, ByteArrayFIFOQueue QUEUE) {
      private Cache() {
         this(new short[256], new ByteArrayFIFOQueue(256));
      }
   }
}

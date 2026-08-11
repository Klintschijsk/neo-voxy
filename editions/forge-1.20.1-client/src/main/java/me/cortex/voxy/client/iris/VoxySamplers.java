package me.cortex.voxy.client.iris;

import net.irisshaders.iris.gl.sampler.GlSampler;
import net.irisshaders.iris.gl.sampler.SamplerHolder;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;

public class VoxySamplers {
    public static void addSamplers(IrisRenderingPipeline pipeline, SamplerHolder samplers) {
        // Oculus 1.20.1 compiles composite programs while IrisRenderingPipeline is
        // still inside its constructor. Register the names unconditionally here;
        // their suppliers stay inert until Voxy's pipeline exists.
            String[] opaqueNames = new String[]{"vxDepthTexOpaque"};
            String[] translucentNames = new String[]{"vxDepthTexTrans"};
            /*
            if (IrisShaderPatch.IMPERSONATE_DISTANT_HORIZONS) {
                opaqueNames = new String[]{"vxDepthTexOpaque", "dhDepthTex1"};
                translucentNames = new String[]{"vxDepthTexTrans", "dhDepthTex", "dhDepthTex0"};
            }*/

            //TODO replace ()->0 with the actual depth texture id
            samplers.addDynamicSampler(TextureType.TEXTURE_2D, () -> {
                var pipeData = ((IGetIrisVoxyPipelineData)pipeline).voxy$getPipelineData();
                if (pipeData == null) {
                    return 0;
                }
                if (pipeData.thePipeline == null) {
                    return 0;
                }

                //In theory the first frame could be null
                var dt = pipeData.thePipeline.fb.getDepthTex();
                if (dt == null) {
                    return 0;
                }
                return dt.id;
            }, new GlSampler(false, true, false, false), opaqueNames);

            samplers.addDynamicSampler(TextureType.TEXTURE_2D, () -> {
                var pipeData = ((IGetIrisVoxyPipelineData)pipeline).voxy$getPipelineData();
                if (pipeData == null) {
                    return 0;
                }
                if (pipeData.thePipeline == null) {
                    return 0;
                }
                //In theory the first frame could be null
                var dt = pipeData.thePipeline.fbTranslucent.getDepthTex();
                if (dt == null) {
                    return 0;
                }
                return dt.id;
            }, new GlSampler(false, true, false, false), translucentNames);
    }
}

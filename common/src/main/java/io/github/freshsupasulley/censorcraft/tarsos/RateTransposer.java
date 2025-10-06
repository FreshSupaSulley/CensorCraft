package io.github.freshsupasulley.censorcraft.tarsos;

public class RateTransposer {

    private double factor;
    private final Resampler resampler;

    /**
     * Create a new sample rate transposer.
     * 
     * @param factor Resampling factor (e.g., 0.5 = half speed, 2.0 = double speed)
     */
    public RateTransposer(double factor) {
        this.factor = factor;
        this.resampler = new Resampler(false, 0.1, 4.0);
    }

    public void setFactor(double factor) {
        this.factor = factor;
    }

    /**
     * Processes a float buffer and returns a resampled version.
     *
     * @param input The input float array (normalized PCM, -1.0 to 1.0)
     * @return The resampled float array
     */
    public float[] process(float[] input) {
        int estimatedLength = (int) (input.length * factor) + 2;
        float[] output = new float[estimatedLength];

        resampler.process(factor, input, 0, input.length, true, output, 0, output.length);

        return output;
    }

    /**
     * Processes a float buffer into a provided output buffer.
     *
     * @param input        Input float array
     * @param output       Output float array
     * @param outputOffset Offset in the output array to start writing
     * @return Number of samples written to output
     */
    public int process(float[] input, float[] output, int outputOffset) {
        return resampler.process(factor, input, 0, input.length, true, output, outputOffset,
                output.length - outputOffset).outputSamplesGenerated;
    }
}
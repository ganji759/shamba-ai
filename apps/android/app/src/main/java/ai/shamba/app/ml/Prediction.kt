package ai.shamba.app.ml

import kotlin.math.exp

/** How sure the model is about its top class. */
enum class Certainty(val key: String) {
    /** top probability >= thresholds.confident. Shown as "Very likely". */
    CONFIDENT("confident"),

    /** between the two thresholds. Shown as "Likely". */
    LIKELY("likely"),

    /** top probability < thresholds.uncertainBelow. Shown as "Not sure". Advice is replaced by retake tips. */
    UNCERTAIN("uncertain"),
    ;

    companion object {
        fun fromKey(key: String?): Certainty = entries.firstOrNull { it.key == key } ?: UNCERTAIN
    }
}

/** Thresholds from manifest.json "thresholds". */
data class Thresholds(val confident: Float = 0.70f, val uncertainBelow: Float = 0.45f) {
    init {
        require(uncertainBelow <= confident) { "uncertain_below must not be above confident" }
    }

    fun certaintyOf(probability: Float): Certainty = when {
        probability.isNaN() -> Certainty.UNCERTAIN
        probability >= confident -> Certainty.CONFIDENT
        probability < uncertainBelow -> Certainty.UNCERTAIN
        else -> Certainty.LIKELY
    }
}

data class ClassScore(val classId: String, val probability: Float)

data class Prediction(
    /** Sorted by probability, highest first. Holds the top class and the runners-up. */
    val ranked: List<ClassScore>,
    val certainty: Certainty,
) {
    val top: ClassScore get() = ranked.first()

    /** The runners-up after the top class. */
    val others: List<ClassScore> get() = ranked.drop(1)
}

object PredictionLogic {
    /**
     * Builds a prediction from the model output.
     * @param output model output, one value per class, in the order of [classes].
     * @param keep number of ranked classes to keep (top class plus runners-up).
     */
    fun predict(output: FloatArray, classes: List<String>, thresholds: Thresholds, keep: Int = 3): Prediction {
        require(classes.isNotEmpty()) { "No classes in the manifest" }
        require(output.size == classes.size) {
            "Model output has ${output.size} values, manifest has ${classes.size} classes"
        }
        val probs = toProbabilities(output)
        val ranked = probs.indices
            .sortedWith(compareByDescending<Int> { probs[it] }.thenBy { it })
            .take(keep.coerceIn(1, classes.size))
            .map { ClassScore(classes[it], probs[it]) }
        return Prediction(ranked, thresholds.certaintyOf(ranked.first().probability))
    }

    /**
     * The model ends in softmax, so the output is already probabilities. If a future model
     * returns logits (negative values, or a sum far from 1), apply softmax here.
     * NaN or infinite values give an all-zero result, which reads as "Not sure".
     */
    fun toProbabilities(output: FloatArray): FloatArray {
        if (output.any { it.isNaN() || it.isInfinite() }) return FloatArray(output.size)
        val sum = output.sum()
        val looksLikeProbs = output.all { it >= 0f && it <= 1f } && sum in 0.98f..1.02f
        if (looksLikeProbs) return output.copyOf()
        val maxLogit = output.max()
        val exps = DoubleArray(output.size) { exp((output[it] - maxLogit).toDouble()) }
        val total = exps.sum()
        return FloatArray(output.size) { (exps[it] / total).toFloat() }
    }
}

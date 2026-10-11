package com.opsflow.knowledge;

import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Turns text into a fixed-length vector, so "how similar are these two texts" becomes
 * "how close are these two vectors".
 *
 * This is feature hashing, not a language model: every word, and every pair of neighbouring
 * words, is hashed to one of DIMENSIONS slots and counted there. It needs no network call or
 * API key, gives the same vector for the same text every time, and is good enough to rank a
 * few hundred runbooks. A learned embedding model understands synonyms; this one only
 * matches shared words. ModelEmbedder is the learned one, switched on with
 * EMBEDDING_PROVIDER=model.
 */
@Service
public class EmbeddingService implements TextEmbedder {

    public static final int DIMENSIONS = 256;

    @Override
    public String name() {
        return "hash-" + DIMENSIONS;
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }

    @Override
    public double minimumScore() {
        return 0.05;   // hashed vectors of unrelated texts are almost exactly at right angles
    }

    @Override
    public float[] embed(String text) {
        float[] vector = new float[DIMENSIONS];
        if (text == null) {
            return vector;
        }
        String[] words = text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        String previous = null;
        for (String word : words) {
            if (word.length() < 3) {
                continue;   // "a", "to", "of": too common to say anything
            }
            add(vector, word, 1.0f);
            if (previous != null) {
                add(vector, previous + " " + word, 0.5f);
            }
            previous = word;
        }
        normalise(vector);
        return vector;
    }

    /** Cosine similarity of two normalised vectors: 1 means the same direction, 0 means nothing shared. */
    public static double cosine(float[] a, float[] b) {
        double dot = 0;
        for (int i = 0; i < a.length && i < b.length; i++) {
            dot += a[i] * b[i];
        }
        return dot;
    }

    private static void add(float[] vector, String feature, float weight) {
        int hash = feature.hashCode();
        int slot = Math.floorMod(hash, DIMENSIONS);
        // the sign comes from a different bit of the hash, so two features that land in the
        // same slot cancel out as often as they add up
        vector[slot] += ((hash >>> 16) & 1) == 0 ? weight : -weight;
    }

    private static void normalise(float[] vector) {
        double sum = 0;
        for (float value : vector) {
            sum += value * value;
        }
        if (sum == 0) {
            return;
        }
        float length = (float) Math.sqrt(sum);
        for (int i = 0; i < vector.length; i++) {
            vector[i] /= length;
        }
    }
}

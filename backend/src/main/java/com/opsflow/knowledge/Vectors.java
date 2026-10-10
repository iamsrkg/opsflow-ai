package com.opsflow.knowledge;

/** Vectors as text, in the "[0.1,0.2,...]" form that both JSON and pgvector accept. */
final class Vectors {

    private Vectors() {
    }

    static String toText(float[] vector) {
        StringBuilder text = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                text.append(',');
            }
            text.append(vector[i]);
        }
        return text.append(']').toString();
    }

    static float[] fromText(String text) {
        String body = text.substring(1, text.length() - 1);
        if (body.isBlank()) {
            return new float[0];
        }
        String[] parts = body.split(",");
        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i].trim());
        }
        return vector;
    }
}

package com.adtdev.ProgressCircle;

/**
 * Created by Anton on 06.06.2018.
 */

public final class DefaultProgressTextAdapter implements ProgressCircle.ProgressTextAdapter {

    @Override
    public String formatText(double currentProgress) {
        return String.valueOf((int) currentProgress);
    }
}

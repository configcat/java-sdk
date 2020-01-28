package com.configcat;

/**
 * The manual polling mode configuration.
 */
class ManualPollingMode extends PollingMode {
    @Override
    String getPollingIdentifier() {
        return "m";
    }

    @Override
    RefreshPolicy accept(PollingModeVisitor visitor) {
        return visitor.visit(this);
    }
}

package com.allinweb.ch.editor;

import org.w3c.dom.events.EventTarget;
import org.w3c.dom.events.MouseEvent;
import org.w3c.dom.views.AbstractView;

public class EventHandler implements MouseEvent {

    @Override
    public int getScreenX() {
        return 0;
    }

    @Override
    public int getScreenY() {
        return 0;
    }

    @Override
    public int getClientX() {
        return 0;
    }

    @Override
    public int getClientY() {
        return 0;
    }

    @Override
    public boolean getCtrlKey() {
        return false;
    }

    @Override
    public boolean getShiftKey() {
        return false;
    }

    @Override
    public boolean getAltKey() {
        return false;
    }

    @Override
    public boolean getMetaKey() {
        return false;
    }

    @Override
    public short getButton() {
        return 0;
    }

    @Override
    public EventTarget getRelatedTarget() {
        return null;
    }

    @Override
    public void initMouseEvent(
            String typeArg,
            boolean canBubbleArg,
            boolean cancelableArg,
            AbstractView viewArg,
            int detailArg,
            int screenXArg,
            int screenYArg,
            int clientXArg,
            int clientYArg,
            boolean ctrlKeyArg,
            boolean altKeyArg,
            boolean shiftKeyArg,
            boolean metaKeyArg,
            short buttonArg,
            EventTarget relatedTargetArg) {}

    @Override
    public AbstractView getView() {
        return null;
    }

    @Override
    public int getDetail() {
        return 0;
    }

    @Override
    public void initUIEvent(
            String typeArg, boolean canBubbleArg, boolean cancelableArg, AbstractView viewArg, int detailArg) {}

    @Override
    public String getType() {
        return null;
    }

    @Override
    public EventTarget getTarget() {
        return null;
    }

    @Override
    public EventTarget getCurrentTarget() {
        return null;
    }

    @Override
    public short getEventPhase() {
        return 0;
    }

    @Override
    public boolean getBubbles() {
        return false;
    }

    @Override
    public boolean getCancelable() {
        return false;
    }

    @Override
    public long getTimeStamp() {
        return 0;
    }

    @Override
    public void stopPropagation() {}

    @Override
    public void preventDefault() {}

    @Override
    public void initEvent(String eventTypeArg, boolean canBubbleArg, boolean cancelableArg) {}
}

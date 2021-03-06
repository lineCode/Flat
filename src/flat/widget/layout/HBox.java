package flat.widget.layout;

import flat.animations.StateInfo;
import flat.graphics.text.Align;
import flat.widget.Widget;
import flat.widget.enuns.Visibility;

import java.util.ArrayList;
import java.util.Collections;

public class HBox extends Box {

    // todo - Horizontal ordering (RTF/LTF)
    private Align.Vertical align;

    ArrayList<Widget> orderedList = new ArrayList<>();

    @Override
    public void applyStyle() {
        super.applyStyle();

        StateInfo info = getStateInfo();

        setAlign(getStyle().asConstant("align", info, Align.Vertical.TOP));
    }

    @Override
    public void onLayout(float x, float y, float width, float height) {
        setLayout(x, y, Math.min(width, getMeasureWidth()), Math.min(getMeasureHeight(), height));
        ArrayList<Widget> children = orderedList;

        float xoff = Math.min(getWidth(), getPaddingLeft() + getMarginLeft()), sum = 0;
        for (Widget child : children) {
            if (child.getVisibility() == Visibility.Gone) continue;

            sum += Math.min(child.getMeasureWidth(), getWidth());
        }

        float mul = 1f;
        if (sum > getWidth()) {
            mul = getWidth() / sum;
        }

        float w = Math.max(0, getWidth() - getPaddingLeft() - getPaddingRight() - getMarginLeft() - getMarginRight());
        float h = Math.max(0, getHeight() - getPaddingTop() - getPaddingBottom() - getMarginTop() - getMarginBottom());
        float reaming = w, sum2 = 0;
        for (Widget child : children) {
            if (child.getVisibility() == Visibility.Gone) continue;

            if (Math.min(child.getMeasureWidth(), w) * mul < Math.min(child.getLayoutMinWidth(), w)) {
                reaming -= Math.min(child.getLayoutMinWidth(), w);
            } else {
                sum2 += Math.min(child.getMeasureWidth(), w);
            }
        }

        float mul2 = reaming / sum2;
        for (Widget child : children) {
            if (child.getVisibility() == Visibility.Gone) continue;

            float childW;
            if (Math.min(child.getMeasureWidth(), w) * mul < Math.min(child.getLayoutMinWidth(), w)) {
                childW = Math.min(child.getLayoutMinWidth(), w);
            } else {
                childW = Math.min(child.getMeasureWidth(), w) * mul2;
            }
            child.onLayout(xoff, yOff(childW), childW, h);
            xoff += child.getWidth();
        }
    }

    @Override
    public void onMeasure() {
        float mWidth = getPrefWidth(), mHeight = getPrefHeight();

        for (Widget child : getChildren()) {
            child.onMeasure();
            if (child.getVisibility() == Visibility.Gone) continue;

            if (mWidth != MATCH_PARENT) {
                if (child.getMeasureWidth() == MATCH_PARENT) {
                    if (getPrefWidth() == WRAP_CONTENT)
                        mWidth = MATCH_PARENT;
                } else {
                    mWidth += child.getMeasureWidth();
                }
            }
            if (mHeight != MATCH_PARENT) {
                if (child.getMeasureHeight() == MATCH_PARENT) {
                    if (getPrefHeight() == WRAP_CONTENT)
                        mHeight = MATCH_PARENT;
                } else if (child.getMeasureHeight() > mHeight) {
                    mHeight = child.getMeasureHeight();
                }
            }
        }
        mWidth += getPaddingLeft() + getPaddingRight() + getMarginLeft() + getMarginRight();
        mHeight += getPaddingTop() + getPaddingBottom() + getMarginTop() + getMarginBottom();
        setMeasure(mWidth, mHeight);
    }

    public void add(Widget child) {
        orderedList.add(child);
        super.add(child);
    }

    public void add(Widget... children) {
        Collections.addAll(orderedList, children);
        super.add(children);
    }

    @Override
    public void childRemove(Widget widget) {
        orderedList.remove(widget);
        super.childRemove(widget);
    }

    public Align.Vertical getAlign() {
        return align;
    }

    public void setAlign(Align.Vertical align) {
        this.align = align;
    }

    private float yOff(float childHeight) {
        float start = getPaddingTop() + getMarginTop();
        float end = getHeight() - getPaddingBottom() - getMarginBottom();
        if (end < start) return (start + end) / 2f;
        if (align == Align.Vertical.BOTTOM || align == Align.Vertical.BASELINE) return end - childHeight;
        if (align == Align.Vertical.MIDDLE) return (start + end - childHeight) / 2f;
        return start;
    }
}

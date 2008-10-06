package com.intellij.execution.ui.layout.impl;

import com.intellij.execution.ui.layout.Tab;
import com.intellij.execution.ui.layout.PlaceInGrid;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;

import javax.swing.*;

public class TabImpl extends AbstractTab implements Tab {

  public TabImpl(Element element) {
    read(element);
  }

  public void read(final Element element) {
    XmlSerializer.deserializeInto(this, element);
  }

  TabImpl() {
  }

  public int getIndex() {
    return myIndex;
  }

  public String getDisplayName() {
    return myDisplayName;
  }

  public Icon getIcon() {
    return myIcon;
  }

  public void setIndex(final int index) {
    myIndex = index;
  }

  public void setDisplayName(final String displayName) {
    myDisplayName = displayName;
  }


  public void write(final Element parentNode) {
    final Element element = XmlSerializer.serialize(this);
    parentNode.addContent(element);
  }

  public float getLeftProportion() {
    return myLeftProportion;
  }

  public void setLeftProportion(final float leftProportion) {
    if (leftProportion < 0 || leftProportion > 1.0) return;
    myLeftProportion = leftProportion;
  }

  public float getRightProportion() {
    return myRightProportion;
  }

  public void setRightProportion(final float rightProportion) {
    if (rightProportion < 0 || rightProportion > 1.0) return;
    myRightProportion = rightProportion;
  }

  public float getBottomProportion() {
    return myBottomProportion;
  }

  public void setBottomProportion(final float bottomProportion) {
    if (bottomProportion < 0 || bottomProportion > 1.0) return;
    myBottomProportion = bottomProportion;
  }

  public boolean isLeftDetached() {
    return myLeftDetached;
  }

  public void setLeftDetached(final boolean leftDetached) {
    myLeftDetached = leftDetached;
  }

  public boolean isCenterDetached() {
    return myCenterDetached;
  }

  public void setCenterDetached(final boolean centerDetached) {
    myCenterDetached = centerDetached;
  }

  public boolean isRightDetached() {
    return myRightDetached;
  }

  public void setRightDetached(final boolean rightDetached) {
    myRightDetached = rightDetached;
  }

  public boolean isBottomDetached() {
    return myBottomDetached;
  }

  public void setBottomDetached(final boolean bottomDetached) {
    myBottomDetached = bottomDetached;
  }

  public boolean isDefault() {
    return myIndex == 0;
  }

  public boolean isDetached(PlaceInGrid place) {
    switch (place) {
      case bottom:
        return isBottomDetached();
      case center:
        return isCenterDetached();
      case left:
        return isLeftDetached();
      case right:
        return isRightDetached();
    }

    return false;
  }

  public void setDetached(PlaceInGrid place, boolean detached) {
    switch (place) {
      case bottom:
        setBottomDetached(detached);
        break;
      case center:
        setCenterDetached(detached);
        break;
      case left:
        setLeftDetached(detached);
        break;
      case right:
        setRightDetached(detached);
        break;
    }
  }

  public static class Default extends AbstractTab {

    public Default(final int index, final String displayName, final Icon icon) {
      myIndex = index;
      myDisplayName = displayName;
      myIcon = icon;
    }

    public TabImpl createTab() {
      final TabImpl tab = new TabImpl();
      tab.copyFrom(this);
      return tab;
    }
  }

}

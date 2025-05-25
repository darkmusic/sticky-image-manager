package com.darkmusic.stickyimagemgr;

public class WinPrefs {
    private int locationX;
    private int locationY;
    private int sizeW;
    private int sizeH;
    private int MIN_WIDTH = 100;
    private int MIN_HEIGHT = 100;
    private int MIN_X = 0;
    private int MIN_Y = 0;

    public int getLocationX() {
        return locationX;
    }

    public void setLocationX(int locationX) {
        if (locationX < MIN_X) {
            locationX = MIN_X;
        }
        this.locationX = locationX;
    }

    public int getLocationY() {
        return locationY;
    }

    public void setLocationY(int locationY) {
        if (locationY < MIN_Y) {
            locationY = MIN_Y;
        }
        this.locationY = locationY;
    }

    public int getSizeW() {
        if (sizeW < MIN_WIDTH) {
            sizeW = MIN_WIDTH;
        }
        return sizeW;
    }

    public void setSizeW(int sizeW) {
        if (sizeW < MIN_WIDTH) {
            sizeW = MIN_WIDTH;
        }
        this.sizeW = sizeW;
    }

    public int getSizeH() {
        if (sizeH < MIN_HEIGHT) {
            sizeH = MIN_HEIGHT;
        }
        return sizeH;
    }

    public void setSizeH(int sizeH) {
        if (sizeH < MIN_HEIGHT) {
            sizeH = MIN_HEIGHT;
        }
        this.sizeH = sizeH;
    }
}

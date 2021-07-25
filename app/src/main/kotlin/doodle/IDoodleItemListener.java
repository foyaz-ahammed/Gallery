package doodle;

/**
 * Created on 19/01/2019.
 */
public interface IDoodleItemListener {
    public int PROPERTY_SCALE = 1;
    public int PROPERTY_ROTATE = 2;
    public int PROPERTY_PIVOT_X = 3;
    public int PROPERTY_PIVOT_Y = 4;
    public int PROPERTY_SIZE = 5;
    public int PROPERTY_COLOR = 6;
    public int PROPERTY_LOCATION = 7;

    /**
     * 속성 변경시 callback
     * @param property 변경된 속성
     */
    void onPropertyChanged(int property);
}

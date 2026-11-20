package org.example.model;

public class HeavyTank extends Tank {

    public HeavyTank(String id, Position position) {
        super(id, position, 26, 4, 6, 2);
    }

    @Override
    public int getShotPower() {
        return 12;
    }

    @Override
    public String getModel() {
        return "Тяжёлый танк";
    }
}


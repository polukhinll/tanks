package org.example.model;

public class LightTank extends Tank {

    public LightTank(String id, Position position) {
        super(id, position, 18, 2, 8, 3);
    }

    @Override
    public int getShotPower() {
        return 9;
    }

    @Override
    public String getModel() {
        return "Лёгкий танк";
    }
}

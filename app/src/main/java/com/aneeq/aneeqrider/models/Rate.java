package com.aneeq.aneeqrider.models;

public class Rate {

    public String rate;
    public String comment;

    public Rate() {
    }

    public Rate(String rate, String comment) {
        this.rate = rate;
        this.comment = comment;
    }

    public String getRate() {
        return rate;
    }

    public void setRate(String rate) {
        this.rate = rate;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}

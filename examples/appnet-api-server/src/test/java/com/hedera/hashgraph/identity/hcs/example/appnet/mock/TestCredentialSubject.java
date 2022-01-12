package com.hedera.hashgraph.identity.hcs.example.appnet.mock;

import com.hedera.hashgraph.identity.hcs.vc.CredentialSubject;
import com.hedera.hashgraph.zeroknowledge.merkletree.MerkleTreeLeaf;

public final class TestCredentialSubject extends CredentialSubject {
    private final String name;
    private final String surname;
    private final int day;
    private final int month;
    private final int year;

    public TestCredentialSubject(String name, String surname, int day, int month, int year) {
        this.name = name;
        this.surname = surname;
        this.day = day;
        this.month = month;
        this.year = year;
    }

    @MerkleTreeLeaf(labelName = "name")
    public String getName() {
        return name;
    }

    @MerkleTreeLeaf(labelName = "surname")
    public String getSurname() {
        return surname;
    }

    @MerkleTreeLeaf(labelName = "day")
    public int getDay() {
        return day;
    }

    @MerkleTreeLeaf(labelName = "month")
    public int getMonth() {
        return month;
    }

    @MerkleTreeLeaf(labelName = "year")
    public int getYear() {
        return year;
    }
}

package org.example;

import java.time.LocalDateTime;

public class Customer {
    private long id;
    private String username;
    private String email;
    private LocalDateTime birthday;

    public Customer(long id, String username, String email, LocalDateTime birthday) {
        this.id = id;
        this.username = username;
        if(checkEmail(email)){
            this.email = email;
        } else {
            throw new IllegalArgumentException("Invalid email");
        }
        if(checkBirthday(birthday)){
            this.birthday = birthday;
        } else {
            throw new IllegalArgumentException("Customer must be at least 18 years old");
        }
    }

    public Customer(Customer other) {
        this(other.id, other.username, other.email, other.birthday);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if(checkEmail(email)){
            this.email = email;
        } else {
            throw new IllegalArgumentException("Invalid email");
        }
    }

    public LocalDateTime getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDateTime birthday) {
        if(checkBirthday(birthday)){
            this.birthday = birthday;
        } else {
            throw new IllegalArgumentException("Customer must be at least 18 years old");
        }
    }

    public long getId() {
        return id;
    }
    private boolean checkEmail(String email) {
        String[] result1 = email.split("@");
        if (result1.length != 2) {
            return false;
        }
        String[] result2 = result1[1].split("\\.");
        if (result2.length < 2) {
            return false;
        }
        if (result2[result2.length - 1].chars().allMatch(Character::isLetter)) {
            return true;
        }
        return false;
    }

    private boolean checkBirthday(LocalDateTime birthday) {
        return birthday.isBefore(LocalDateTime.now().minusYears(18));
    }
    @Override
    public String toString() {
        return "Customer:" +
                "id: " + id + '\n' +
                "username: " + username + '\n' +
                "email: " + email + '\n' +
                "birthday: " + birthday;
    }
}

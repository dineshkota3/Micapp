package com.example.dineshkota.randd;

/**
 * Created by dineshkota on 5/9/15.
 */

// this class is used to represent the students who are connected to prof which we can see in the ProfReceiveActivity as list.
public class Student {
    public String name;
    public String ip;
    public Boolean active;

    public Student(String name,String ip){
        this.name = name;
        //indicates the name of the student
        this.ip = ip;
        //ip of the student.
        active = false;
        //this active indicates whether the student is allowed to speak or not.
    }
}

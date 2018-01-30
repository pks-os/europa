/*
  $Id: $
  @file Permission.java
  @brief Contains the Permission.java class

  @author Rahul Singh [rsingh]
  Copyright (c) 2013, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa.security;

public enum Permission
{
    FULLCONTROL,  //Full Access to the resource
    PUSH,         //Push to a repo
    PULL,         //Pull from a repo
    CREATE,       //Create a local repo
    DELETE,       //Delete a repo
    LIST,         //List repos, pipelines etc
    CONNECT,      //Connect a remote repo
    DISCONNECT,   //Disconnect a remote repo
    READ,         //READ / View repo page
    MODIFY,       //WRITE / MODIFY a repo (settings, overview etc)
    NONE;         //Dummy permission to represent no permissions
}

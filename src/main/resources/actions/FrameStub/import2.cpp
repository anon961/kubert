if({{ debug }}) { printf("[FrameStub] import called\n"); fflush(stdout); }

if(id.isValid()) {
    char partName[64], targetId[64], cmd[1024];

    int i;
    for(i=0; i<strlen(destPart->role()->name); i++) {
        char c = destPart->role()->name[i];
        if(c == '-')
            partName[i] = '_';
        else
            partName[i] = toupper(c);
    }
    partName[i] = '\0';

    for(i=0; i<strlen(id.getId()); i++) {
        char c = id.getId()[i];
        if(c == '-')
            targetId[i] = '_';
        else
            targetId[i] = toupper(c);
    }
    targetId[i] = '\0';

    sprintf(cmd, "kubectl set env deployments/{{ ownerFQN }}-%s%d {{ ownerFQNCapital }}_%s%d_SERVICE_HOST=$%s_SERVICE_HOST {{ ownerFQNCapital }}_%s%d_SERVICE_PORT_%s_%d=$%s_SERVICE_PORT_PLUGIN_0",
        destPart->role()->name, index, partName, index, targetId, partName, index, partName, index, targetId);
    if({{ debug }}) { printf("[FrameStub] running command '%s'\n", cmd); fflush(stdout); }
    system(cmd);

    sprintf(cmd, "kubectl scale deployments/{{ ownerFQN }}-%s%d --replicas=1", destPart->role()->name, index);
    if({{ debug }}) { printf("[FrameStub] running command '%s'\n", cmd); fflush(stdout); }
    system(cmd);

    UMLRTCapsuleId realId = framePort->incarnate(destPart);
    if(realId.isValid()) {
        if({{ debug }}) { printf("[FrameStub] import incarnated capusle '%s'\n", realId.getCapsule()->getName()); fflush(stdout); }
        ((UMLRTCapsuleId&)id).setCapsule(realId.getCapsule());
        if({{ debug }}) { printf("[FrameStub] after setCapsule'%s'\n", id.getCapsule()->getName()); fflush(stdout); }

        return true;
    }
}

return false;
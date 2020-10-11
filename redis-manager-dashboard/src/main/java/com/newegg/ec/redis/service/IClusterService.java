package com.newegg.ec.redis.service;

import com.newegg.ec.redis.entity.Cluster;
import com.newegg.ec.redis.plugin.install.service.AbstractNodeOperation;

import java.util.List;

/**
 * @author Jay.H.Zou
 * @date 7/19/2019
 */
public interface IClusterService {

    List<Cluster> getAllClusterList();

    List<Cluster> getClusterListByGroupId(Integer groupId);

    Cluster getClusterById(Integer clusterId);

    Cluster getClusterByIdAndGroup(Integer groupId, Integer clusterId);

    Cluster getClusterByName(String clusterName);

    Cluster completeClusterInfo(Cluster cluster);

    boolean addCluster(Cluster cluster);

    boolean updateClusterMeta(Cluster cluster);

    boolean updateClusterState(Cluster cluster);

    boolean updateNodes(Cluster cluster);

    boolean updateClusterRuleIds(Cluster cluster);

    boolean updateClusterChannelIds(Cluster cluster);

    boolean deleteCluster(Integer clusterId);

    AbstractNodeOperation getNodeOperation(Integer installationEnvironment);

}

package com.newegg.ec.redis.service.impl;

import com.newegg.ec.redis.dao.IGroupDao;
import com.newegg.ec.redis.dao.IGroupUserDao;
import com.newegg.ec.redis.dao.IUserDao;
import com.newegg.ec.redis.entity.Group;
import com.newegg.ec.redis.entity.User;
import com.newegg.ec.redis.service.IUserService;
import com.newegg.ec.redis.util.ImageUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static com.newegg.ec.redis.config.SystemConfig.AVATAR_PATH;

/**
 * @author Jay.H.Zou
 * @date 2019/9/2
 */
@Service
public class UserService implements IUserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private IUserDao userDao;

    @Autowired
    private IGroupUserDao groupUserDao;

    @Autowired
    private IGroupDao groupDao;

    @Override
    public List<User> getAllUser() {
        try {
            return userDao.selectAllUser();
        } catch (Exception e) {
            logger.error("Get all user failed.", e);
            return null;
        }
    }

    @Override
    public List<User> getUserByGroupId(Integer groupId) {
        try {
            List<User> users = userDao.selectUserByGroupId(groupId);
            return users;
        } catch (Exception e) {
            logger.error("Get user by group id failed.", e);
            return null;
        }
    }

    @Override
    public List<User> getGrantUserByGroupId(Integer grantGroupId) {
        try {
            return userDao.selectGrantUserByGroupId(grantGroupId);
        } catch (Exception e) {
            logger.error("Get grant user by group id failed.", e);
            return null;
        }
    }

    @Override
    public User getUserByNameAndPassword(User user) {
        try {
        	user.setPassword(IUserDao.encoderByMd5(user.getPassword()));
            User userLogin = userDao.selectUserByNameAndPassword(user);
            if (userLogin == null) {
                return null;
            }
            User userRole = getUserRole(userLogin.getGroupId(), userLogin.getUserId());
            userLogin.setUserRole(userRole.getUserRole());
            userLogin.setPassword(null);
            userLogin.setToken(null);
            return userLogin;
        } catch (Exception e) {
            logger.error("Get user by user name and password failed, " + user, e);
            return null;
        }
    }

    @Override
    public User getUserById(Integer userId) {
        try {
            return userDao.selectUserById(userId);
        } catch (Exception e) {
            logger.error("Get user by id failed.", e);
            return null;
        }
    }

    @Override
    public User getUserRole(Integer grantGroupId, Integer userId) {
        try {
            return userDao.selectUserRole(grantGroupId, userId);
        } catch (Exception e) {
            logger.error("Get user by group id and user id failed.", e);
            return null;
        }
    }

    @Transactional
    @Override
    public boolean addUser(User user) {
    	try {
			user.setPassword(IUserDao.encoderByMd5(user.getPassword()));
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			logger.error("error md5: user.getPassword()={}", user.getPassword());
            return false;
		}
        userDao.insertUser(user);
        String avatar = AVATAR_PATH + ImageUtil.getImageName(user.getUserId());
        user.setAvatar(avatar);
        updateUserAvatar(user);
        if (User.UserRole.SUPER_ADMIN == user.getUserRole()) {
            List<Group> groupList = groupDao.selectAllGroup();
            for (Group group : groupList) {
                User groupUser = new User();
                groupUser.setUserId(user.getUserId());
                groupUser.setGroupId(user.getGroupId());
                groupUser.setUserRole(User.UserRole.SUPER_ADMIN);
                groupUserDao.insertGroupUser(groupUser, group.getGroupId());
            }
        } else {
            groupUserDao.insertGroupUser(user, user.getGroupId());
        }
        return true;
    }

    @Override
    public boolean isGranted(Integer grantGroupId, Integer userId) {
        try {
            User granted = groupUserDao.isGranted(grantGroupId, userId);
            return granted != null;
        } catch (Exception e) {
            logger.error("", e);
            return true;
        }
    }

    @Override
    public boolean grantUser(User user) {
        try {
            Integer grantGroupId = user.getGroupId();
            User userByName = getUserByName(user.getUserName());
            userByName.setUserRole(user.getUserRole());
            return groupUserDao.insertGroupUser(userByName, grantGroupId) > 0;
        } catch (Exception e) {
            logger.error("Grant user failed.", e);
            return false;
        }
    }

    @Override
    public User getUserByName(String userName) {
        try {
            return userDao.selectUserByName(userName);
        } catch (Exception e) {
            logger.error("Get user by user name failed, user name = " + userName, e);
            return new User();
        }
    }

    @Transactional
    @Override
    public boolean updateUser(User user) {
    	try {
			user.setPassword(IUserDao.encoderByMd5(user.getPassword()));
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			logger.error("error md5: user.getPassword()={}", user.getPassword());
            return false;
		}
        userDao.updateUser(user);
        groupUserDao.updateUserRole(user);
        return true;
    }

    @Override
    public boolean updateUserAvatar(User user) {
        try {
            return userDao.updateUserAvatar(user) > 0;
        } catch (Exception e) {
            logger.error("update user avatar failed", e);
            return false;
        }
    }

    @Transactional
    @Override
    public boolean deleteUserById(Integer userId) {
        userDao.deleteUserById(userId);
        groupUserDao.deleteGroupUserByUserId(userId);
        return true;
    }

    @Override
    public boolean revokeUser(User user) {
        try {
            return groupUserDao.deleteGroupUserByGrantGroupId(user.getGroupId(), user.getUserId()) > 0;
        } catch (Exception e) {
            logger.error("Revoke user failed.", e);
            return false;
        }
    }

    @Override
    public boolean deleteUserByGroupId(Integer groupId) {
        try {
            userDao.deleteUserByGroupId(groupId);
            return true;
        } catch (Exception e) {
            logger.error("Delete users by group id failed, group id = " + groupId, e);
            return false;
        }
    }

    @Override
    public Integer getUserNumber(Integer groupId) {
        if (groupId == null) {
            return 0;
        }
        try {
            return userDao.selectUserNumber(groupId);
        } catch (Exception e) {
            logger.error("Get user number failed, ", e);
            return 0;
        }
    }
}

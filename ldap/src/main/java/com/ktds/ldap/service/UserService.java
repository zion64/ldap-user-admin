/*
 * Copyright 2005-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ktds.ldap.service;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.support.BaseLdapNameAware;

import com.ktds.ldap.domain.DirectoryType;
import com.ktds.ldap.domain.Group;
import com.ktds.ldap.domain.GroupRepo;
import com.ktds.ldap.domain.User;
import com.ktds.ldap.domain.UserRepo;

import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.ldap.support.LdapUtils;

import javax.naming.Name;
import javax.naming.ldap.LdapName;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Mattias Hellborg Arthursson
 */
public class UserService implements BaseLdapNameAware {
	
	private static final Logger logger = LoggerFactory.getLogger("com.ktds.ldap");
	
	private final UserRepo userRepo;
	private final GroupRepo groupRepo;
	private LdapName baseLdapPath;
	private DirectoryType directoryType;

	@Autowired
	public UserService(UserRepo userRepo, GroupRepo groupRepo) {
		this.userRepo = userRepo;
		this.groupRepo = groupRepo;
	}

	public Group getUserGroup() {
		return groupRepo.findByName(GroupRepo.USER_GROUP);
	}

	public void setDirectoryType(DirectoryType directoryType) {
		this.directoryType = directoryType;
	}

	@Override
	public void setBaseLdapPath(LdapName baseLdapPath) {
		this.baseLdapPath = baseLdapPath;
	}

	public Iterable<User> findAll() {
		logger.info("UserService.findAll");
		return userRepo.findAll();
	}

	public User findUser(String userId) {
		logger.info("사용자를 찾으러 갑니다. 아이디는 다음과 같습니다. {}", userId);
		return userRepo.findOne(LdapUtils.newLdapName(userId));
	}

	public User createUser(User user) {
		logger.info("사용자를 생성하러 갑니다. 사원번호는 다음과 같습니다. {} ", user.getEmployeeNumber());
		User savedUser = userRepo.save(user);

		Group userGroup = getUserGroup();

		// The DN the member attribute must be absolute
		userGroup.addMember(toAbsoluteDn(savedUser.getId()));
		groupRepo.save(userGroup);

		return savedUser;
	}

	public LdapName toAbsoluteDn(Name relativeName) {
		return LdapNameBuilder.newInstance(baseLdapPath).add(relativeName).build();
	}

	/**
	 * This method expects absolute DNs of group members. In order to find the
	 * actual users the DNs need to have the base LDAP path removed.
	 * 
	 * @param absoluteIds
	 * @return User set
	 */
	public Set<User> findAllMembers(Iterable<Name> absoluteIds) {
		logger.info("모든 구성원을 찾으러 갑니다.");
		return Sets.newLinkedHashSet(userRepo.findAll(toRelativeIds(absoluteIds)));
	}

	public Iterable<Name> toRelativeIds(Iterable<Name> absoluteIds) {
		return Iterables.transform(absoluteIds, new Function<Name, Name>() {
			@Override
			public Name apply(Name input) {
				return LdapUtils.removeFirst(input, baseLdapPath);
			}
		});
	}

	public User updateUser(String userId, User user) {
		logger.info("사용자 정보를 갱신합니다. 사용자아이디와 기타 정보는 다음과 같습니다. \n사용자ID: {}\n이름: {}\n소속부서: {}", userId, user.getFullName(), user.getDepartment());
		LdapName originalId = LdapUtils.newLdapName(userId);
		User existingUser = userRepo.findOne(originalId);

		existingUser.setFirstName(user.getFirstName());
		existingUser.setLastName(user.getLastName());
		existingUser.setFullName(user.getFullName());
		existingUser.setEmail(user.getEmail());
		existingUser.setPhone(user.getPhone());
		existingUser.setTitle(user.getTitle());
		existingUser.setDepartment(user.getDepartment());
		existingUser.setUnit(user.getUnit());

		if (directoryType == DirectoryType.AD) {
			return updateUserAd(originalId, existingUser);
		} else {
			return updateUserStandard(originalId, existingUser);
		}
	}

	/**
	 * Update the user and - if its id changed - update all group references to
	 * the user.
	 * 
	 * @param originalId
	 *            the original id of the user.
	 * @param existingUser
	 *            the user, populated with new data
	 * 
	 * @return the updated entry
	 */
	private User updateUserStandard(LdapName originalId, User existingUser) {
		User savedUser = userRepo.save(existingUser);

		if (!originalId.equals(savedUser.getId())) {
			// The user has moved - we need to update group references.
			LdapName oldMemberDn = toAbsoluteDn(originalId);
			LdapName newMemberDn = toAbsoluteDn(savedUser.getId());

			Collection<Group> groups = groupRepo.findByMember(oldMemberDn);
			updateGroupReferences(groups, oldMemberDn, newMemberDn);
		}
		return savedUser;
	}

	/**
	 * Special behaviour in AD forces us to get the group membership before the
	 * user is updated, because AD clears group membership for removed entries,
	 * which means that once the user is update we've lost track of which groups
	 * the user was originally member of, preventing us to update the membership
	 * references so that they point to the new DN of the user.
	 * 
	 * This is slightly less efficient, since we need to get the group
	 * membership for all updates even though the user may not have been moved.
	 * Using our knowledge of which attributes are part of the distinguished
	 * name we can do this more efficiently if we are implementing specifically
	 * for Active Directory - this approach is just to highlight this quite
	 * significant difference.
	 * 
	 * @param originalId
	 *            the original id of the user.
	 * @param existingUser
	 *            the user, populated with new data
	 * 
	 * @return the updated entry
	 */
	private User updateUserAd(LdapName originalId, User existingUser) {
		LdapName oldMemberDn = toAbsoluteDn(originalId);
		Collection<Group> groups = groupRepo.findByMember(oldMemberDn);

		User savedUser = userRepo.save(existingUser);
		LdapName newMemberDn = toAbsoluteDn(savedUser.getId());

		if (!originalId.equals(savedUser.getId())) {
			// The user has moved - we need to update group references.
			updateGroupReferences(groups, oldMemberDn, newMemberDn);
		}
		return savedUser;
	}

	private void updateGroupReferences(Collection<Group> groups, Name originalId, Name newId) {
		for (Group group : groups) {
			group.removeMember(originalId);
			group.addMember(newId);

			groupRepo.save(group);
		}
	}

	public List<User> searchByNameName(String lastName) {
		return userRepo.findByFullNameContains(lastName);
	}
}

package com.ktds.ldap.web;

//import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
//import static org.springframework.web.bind.annotation.RequestMethod.GET;
//import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.ktds.ldap.domain.DepartmentRepo;
import com.ktds.ldap.domain.Group;
import com.ktds.ldap.domain.GroupRepo;
import com.ktds.ldap.domain.User;
import com.ktds.ldap.service.UserService;

/**
 * Handles requests for the application home page.
 */
@Controller
public class HomeController {

	private static final Logger logger = LoggerFactory.getLogger("com.ktds.ldap");
	private final AtomicInteger nextEmployeeNumber = new AtomicInteger(10);
	
	@Autowired
	private DepartmentRepo departmentRepo;
	
	@Autowired
	private GroupRepo groupRepo;
	
	@Autowired
	private UserService userService;
	
	/**
	 * Simply selects the home view to render by returning its name.
	 */
//	@RequestMapping(value = "/testing", method = RequestMethod.GET)
//	public String home(Locale locale, Model model) {
//		logger.info("환영합니다! 로깅합니다. The client locale is {}.", locale);
//
//		Date date = new Date();
//		DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, locale);
//
//		String formattedDate = dateFormat.format(date);
//
//		model.addAttribute("serverTime", formattedDate);
//
//		return "home";
//	}

	@RequestMapping(value = {"/", "/users" }, method = RequestMethod.GET)
	public String index(ModelMap map, @RequestParam(required = false) String name) {
		logger.info("사용자 목록을 얻어 옮니다.(/users)");
		if (StringUtils.hasText(name)) {
			logger.info("사용자 목록을 얻어 옮니다.(hasText = true)");
			map.put("users", userService.searchByNameName(name));
		} else {
			logger.info("사용자 목록을 얻어 옮니다.(hasText = false)");
			map.put("users", userService.findAll());
		}
		return "listUsers";
	}

	@RequestMapping(value = "/users/{userid}", method = RequestMethod.GET)
	public String getUser(@PathVariable String userid, ModelMap map) throws JsonProcessingException {
//		map.put("new", false);
		map.put("user", userService.findUser(userid));
		populateDepartments(map);
		logger.info("선택한 사용자의 정보를 얻어옮니다. 선택된 사용자는 /users/[{}]이고 소속부서는 [{}] 입니다.", userid, userService.findUser(userid).getDepartment());
		return "editUser";
	}

	@RequestMapping(value = "/newuser", method = RequestMethod.GET)
	public String initNewUser(ModelMap map) throws JsonProcessingException {
		logger.info("새로운 LDAP사용자를 입력받기 위한 폼을 준비합니다. ");
		User user = new User();
		
		user.setEmployeeNumber(nextEmployeeNumber.getAndIncrement());
		
//		map.put("new", true);
//		logger.info("new에 할당된 값은 [{}]입니다.", map.get("new"));
		map.put("user", user);
		
		populateDepartments(map);

		return "newUser";
	}

	private void populateDepartments(ModelMap map) throws JsonProcessingException {
		Map<String, List<String>> departmentMap = departmentRepo.getDepartmentMap();
		ObjectMapper objectMapper = new ObjectMapper();
		String departmentsAsJson = objectMapper.writeValueAsString(departmentMap);
		logger.info("부서목록을 얻으러 갑니다. {}", departmentsAsJson);
		map.put("departments", departmentsAsJson);
	}

	@RequestMapping(value = "/newuser", method = RequestMethod.POST)
	public String createUser(User user) {
		logger.info("사용자를 생성합니다. 사용자이름은 [{}]입니다.", user.getFullName());
		User savedUser = userService.createUser(user);

		return "redirect:/users/" + savedUser.getId();
	}

	@RequestMapping(value = "/users/{userid}", method = RequestMethod.POST)
	public String updateUser(@PathVariable String userid, User user) {
		logger.info("(updateUser)환영합니다! 로깅합니다. This is /users/userid:{}. POST", userid);
		User savedUser = userService.updateUser(userid, user);

		return "redirect:/users/" + savedUser.getId();
	}

    @RequestMapping(value = "/groups", method = RequestMethod.GET)
    public String listGroups(ModelMap map) {
        map.put("groups", groupRepo.getAllGroupNames());
        return "listGroups";
    }

    @RequestMapping(value = "/newGroup", method = RequestMethod.GET)
    public String initNewGroup() {
        return "newGroup";
    }

    @RequestMapping(value = "/groups", method = RequestMethod.POST)
    public String newGroup(Group group) {
        groupRepo.create(group);

        return "redirect:groups/" + group.getName();
    }

    @RequestMapping(value = "/groups/{name}", method = RequestMethod.GET)
    public String editGroup(@PathVariable String name, ModelMap map) {
        Group foundGroup = groupRepo.findByName(name);
        map.put("group", foundGroup);

        final Set<User> groupMembers = userService.findAllMembers(foundGroup.getMembers());
        map.put("members", groupMembers);

        Iterable<User> otherUsers = Iterables.filter(userService.findAll(), new Predicate<User>() {
            @Override
            public boolean apply(User user) {
                return !groupMembers.contains(user);
            }
        });
        map.put("nonMembers", Lists.newLinkedList(otherUsers));

        return "editGroup";
    }

    @RequestMapping(value = "/groups/{name}/members", method = RequestMethod.POST)
    public String addUserToGroup(@PathVariable String name, @RequestParam String userId) {
        Group group = groupRepo.findByName(name);
        group.addMember(userService.toAbsoluteDn(LdapUtils.newLdapName(userId)));

        groupRepo.save(group);

        return "redirect:/groups/" + name;
    }

    @RequestMapping(value = "/groups/{name}/members", method = RequestMethod.DELETE)
    public String removeUserFromGroup(@PathVariable String name, @RequestParam String userId) {
        Group group = groupRepo.findByName(name);
        group.removeMember(userService.toAbsoluteDn(LdapUtils.newLdapName(userId)));

        groupRepo.save(group);

        return "redirect:/groups/" + name;
    }
}

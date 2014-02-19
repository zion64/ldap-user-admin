<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<head>
<title>Spring LDAP User Admin</title>
<link rel="icon" type="image/x-icon" href="<c:url value='/resources/img/favicon.png'/>" />
<link href="<c:url value='/resources/bootstrap/css/bootstrap.min.css'/>" rel="stylesheet" />
<link href="<c:url value='/resources/css/default.css'/>" rel="stylesheet" />
<c:url var="jqueryUrl" value="/resources/jquery/jquery.min.js" />
<script src="${jqueryUrl}"></script>
<c:url var="bootstrapJsUrl" value="/resources/bootstrap/js/bootstrap.min.js" />
<script src="${bootstrapJsUrl}"></script>
<c:url var="underscoreJsUrl" value="/resources/underscore/underscore.min.js" />
<script src="${underscoreJsUrl}"></script>

<link rel="stylesheet" type="text/css" href="<c:url value='/resources/gridforms/gridforms.css'/>">
<c:url var="gridformsJsURL" value="/resources/gridforms/gridforms.js" />
<script src="${gridformsJsURL}"></script>
<meta name="viewport" content="initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no">
<style type="text/css">
body {
	font-family: 나눔고딕, sans-serif
}
</style>

<script type="text/javascript">
	var activeNav = ".users-nav";

	$(document).ready(function() {
		$(activeNav).addClass("active");
	});
</script>
</head>
<body>

	<div class="navbar navbar-inverse navbar-fixed-top">
		<div class="container">
			<div class="navbar-header">
				<button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
					<span class="icon-bar"></span> <span class="icon-bar"></span> <span class="icon-bar"></span>
				</button>
				<a class="navbar-brand" href="http://projects.spring.io/spring-ldap/">Spring LDAP</a>
			</div>
			<div class="collapse navbar-collapse">
				<ul class="nav navbar-nav">
					<li class="users-nav"><a href="<c:url value='/users'/>">Users</a></li>
					<li class="groups-nav"><a href="<c:url value='/groups'/>">Groups</a></li>
				</ul>
			</div>
			<!--/.nav-collapse -->
		</div>
	</div>
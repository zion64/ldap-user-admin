<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ page session="false"%>
<c:import url="header.jsp" />

	<P>The time on the server is ${serverTime}.</P>

<div class="container">
    <div class="main-body item-list">
        <div class="row">
            <div class="col-md-2">
                <h2>Users</h2>
            </div>
            <div class="col-md-10">
                <form method="GET" role="form">
                    <div class="row search-form">
                        <div class="col-md-offset-6 col-md-4">
                            <input type="text" class="form-control" id="name" name="name"/>
                        </div>
                        <div class="col-md-1">
                            <button type="submit" class="btn btn-default">Filter</button>
                        </div>
                    </div>
                </form>
            </div>
        </div>
        <c:forEach var="user" items="${users}" varStatus="stat">
            <div class="row row-${stat.index % 2}">
                <div class="col-md-11">${user.fullName} - ${user.department}&nbsp;</div>
                <div class="col-md-1"><a href="<c:url value='/users/${user.id}'/>">Edit</a></div>
            </div>
        </c:forEach>
        <div class="button-bottom">
            <div class="col-md-offset-10 col-md-1"><a class="btn btn-primary" href="<c:url value='/newuser' />" role="button">New User</a></div>
        </div>
    </div>
</div>

<c:import url="footer.jsp" />
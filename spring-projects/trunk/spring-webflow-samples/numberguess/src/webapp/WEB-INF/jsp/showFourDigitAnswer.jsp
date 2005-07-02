<%@ include file="includeTop.jsp" %>

<div id="content">
	<div id="insert"><img src="images/webflow-logo.jpg"/></div>
	<h2>Show answer</h2>
	<table>
	<tr>
		<td>Total number of guesses:</td>
		<td>${data.guesses}</td>
	</tr>
	<tr>
		<td>Elapsed time in seconds:</td>
		<td>${data.durationSeconds}</td>
	</tr>
	<tr>
		<td>Answer:</td>
		<td>${data.answer}</td>
	</tr>
	<tr>
		<td colspan="2" class="buttonBar">
			<form action="play.htm">
				<input type="hidden" name="_flowId" value="fourDigitNumberGuess">
				<input type="submit" value="Play Again!">
			</form>
		</td>
	</tr>
	
<%@ include file="includeBottom.jsp" %>
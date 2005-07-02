<%@ include file="includeTop.jsp" %>

<div id="content">
	<div id="insert"><img src="images/webflow-logo.jpg"/></div>
	<h2>The Number Guess Game: Guess a number between 1 and 100!</h2>
	<hr>
	<p>Number of guesses so far: ${data.guesses}</p>
	<form name="guessForm" method="post">
		<table>
		    <tr>
		    	<td>Guess:</td>
		    	<td>
		    		<input type="text" name="guess" value="${param.guess}">
		    	</td>
		    </tr>
			<tr>
				<td colspan="2" class="buttonBar">
					<input type="hidden" name="_flowExecutionId" value="${flowExecutionId}">
					<input type="submit" class="button" name="_eventId_submit" value="Guess">
				</td>
			</tr>		    
		</table>
	</form>
</div>

<%@ include file="includeBottom.jsp" %>

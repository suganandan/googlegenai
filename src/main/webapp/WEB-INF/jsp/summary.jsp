<!DOCTYPE html>

<html xmlns:th="http://www.thymeleaf.org">



<head>

<title>Generative AI</title>

<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />



<link rel="stylesheet" type="text/css"
	href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css" />

<style type="text/css">
.scroller {
	width: 900px;
	height: 400px;
	scrollbar-width: thin;
}
</style>

</head>

<script src="https://code.jquery.com/jquery-1.10.2.js"
	type="text/javascript"></script>

<script>
	$(document).ready(function() {

		$("#btnSubmit").click(function(event) {

			//stop submit the form, we will post it manually.

			event.preventDefault();

			fire_ajax_submit();

		});

	});

	function fire_ajax_submit() {

		var form = $('#fileUploadForm')[0];

		var data = new FormData(form);

		data.append("CustomField", "This is some extra data, testing");

		$("#btnSubmit").prop("disabled", true);

		$.ajax({

			type : "POST",

			enctype : 'multipart/form-data',

			url : "/api/summary",

			data : data,

			processData : false, //prevent jQuery from automatically transforming the data into a query string

			contentType : false,

			cache : false,

			timeout : 600000,

			success : function(data) {

				$('#feedback').html("<pre>" + data + "</pre>");

				console.log("SUCCESS : ", data);

				$("#btnSubmit").prop("disabled", false);

			},

			error : function(e) {

				$("#feedback").text(e.responseText);

				console.log("ERROR : ", e);

				$("#btnSubmit").prop("disabled", false);

			}

		});

	}
</script>

<body>



	<nav class="navbar navbar-inverse">

		<div class="container">

			<div class="navbar-header">

				<a class="navbar-brand" href="/">Generative AI </a><label
					class="navbar-brand">- Summary Prompt</label>

			</div>

		</div>



	</nav>





	<div class="container" style="min-height: 500px">



		<div class="starter-template">

			<form method="POST" enctype="multipart/form-data" id="fileUploadForm">

				<div class="col-sm-offset-2 col-sm-10">

					<table>

						<tr>

							<td><input class="form-control " type="file" name="files" /></td>

							<td><input type="submit" value="Submit"
								class="btn btn-primary" id="btnSubmit" /></td>

						</tr>

					</table>

				</div>

			</form>





			<div id="feedback"></div>

		</div>



	</div>
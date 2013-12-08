<!--
  To change this template, choose Tools | Templates
  and open the template in the editor.
-->

<%@ page contentType="text/html;charset=UTF-8" %>

<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Challenge2</title>
    <g:javascript library='jquery' />
    
		<r:layoutResources />
                
  </head>
  <body>
    <h1> <div id="testme">Map</div></h1>
    <canvas id="myCanvas" width="600" height="400"></canvas>
    <script>
      window.setInterval(function(){
        var canvas = document.getElementById('myCanvas');
        var context = canvas.getContext('2d');
         //clear canvas
         context.clearRect(0, 0, canvas.width, canvas.height);
         
         //fetch it
         $.getJSON("/challenge8/coord/getCoords",function(data){
           //draw from the data
           console.log(data);
           context.fillRect(data.x,data.y,10,10);
         });
         
      },1000);
      
      </script>
  </body>
</html>

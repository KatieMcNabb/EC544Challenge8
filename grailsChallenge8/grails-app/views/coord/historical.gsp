<!--
  To change this template, choose Tools | Templates
  and open the template in the editor.
-->

<%@ page contentType="text/html;charset=UTF-8" %>

<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Historical</title>
    <g:javascript library='jquery' />
    
		<r:layoutResources />
                <g:javascript src="highcharts.js" />
  </head>
  <body>
    <script>
      var datamote1;
      var datamote2;
      var datamote3;
      $.getJSON("/challenge2/chart/mote1", function( data1 ) {
        datamote1 = data1;
        $.getJSON("/challenge2/chart/mote2", function( data2 ) {
          datamote2 = data2;
          $.getJSON("/challenge2/chart/mote3", function( data3 ) {
            datamote3 = data3;
            Highcharts.setOptions({global: { useUTC: false } });
        $(function () {
        $('#container').highcharts({type: 'spline',
            title: {
                text: 'Historical Temperatures',
                x: -20 //center
            },
            
            xAxis: {
                type: 'datetime',
                tickPixelInterval: 150
            },
            yAxis: {
                title: {
                    text: 'Temperature (°F)'
                },
                plotLines: [{
                    value: 0,
                    width: 1,
                    color: '#808080'
                }]
            },
            tooltip: {
                valueSuffix: '°F'
            },
            legend: {
                layout: 'vertical',
                align: 'right',
                verticalAlign: 'middle',
                borderWidth: 0
            },
            series: [{
                name: 'Mote1',
                data: (function() {
                    // generate an array of random data
                    var data = [],
                        time = (new Date()).getTime(),
                        i;
    
                    for (i=0; i<datamote1.length; i+=1) {
                        data.push({
                            x: new Date(datamote1[i].time),
                            y: parseFloat(datamote1[i].temperature)
                        });
                    }
                    return data;
                })()
            }, {
                name: 'Mote2',
                data: (function() {
                    // generate an array of random data
                    var data = [],
                        time = (new Date()).getTime(),
                        i;
    
                    for (i=0; i<datamote2.length; i+=1) {
                        data.push({
                            x: new Date(datamote2[i].time),
                            y: parseFloat(datamote2[i].temperature)
                        });
                    }
                    return data;
                })()
            }, {
                name: 'Mote3',
                data: (function() {
                    // generate an array of random data
                    var data = [],
                        time = (new Date()).getTime(),
                        i;
    
                    for (i=0; i<datamote3.length; i+=1) {
                        data.push({
                            x: new Date(datamote3[i].time),
                            y: parseFloat(datamote3[i].temperature)
                        });
                    }
                    return data;
                })()
            }]
        });
    });
    });
    });
    });
      </script>
    <div id="container" style="min-width: 310px; height: 400px; margin: 0 auto"></div>
  </body>
</html>

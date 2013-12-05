<!--
  To change this template, choose Tools | Templates
  and open the template in the editor.
-->

<%@ page contentType="text/html;charset=UTF-8" %>

<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Live Data</title>
    <g:javascript library='jquery' />
    
		<r:layoutResources />
                <g:javascript src="highcharts.js" />
                <script>
                  var mote1object = {};
                          var mote2object = {};
                          var mote3object ={};
                  var time = 0;
                $(function () {
    $(document).ready(function() {
        Highcharts.setOptions({
            global: {
                useUTC: false
            }
        });
    
        var chart;
        $('#container').highcharts({
            chart: {
                type: 'spline',
                animation: Highcharts.svg, // don't animate in old IE
                marginRight: 10,
                events: {
                    load: function() {
    
                        // set up the updating of the chart each second
                        var series = this.series[0];
                        var series2 = this.series[1];
                        var series3 = this.series[2];
                        
                        setInterval(function() {
                          
                          $.getJSON("/challenge2/chart/nexttime1", function( data ) {
                              mote1object = data;
                              console.log("latest time: " + new Date(mote1object.time) )
                              series.addPoint([(new Date()).getTime(), parseFloat(mote1object.temperature)], true, true);
                              
                          });
                          $.getJSON("/challenge2/chart/nexttime2", function( data ) {
                              mote2object = data;
                              series2.addPoint([(new Date()).getTime(), parseFloat(mote2object.temperature)], true, true);
                          });
                          $.getJSON("/challenge2/chart/nexttime3", function( data ) {
                              mote3object = data;
                              series3.addPoint([(new Date()).getTime(), parseFloat(mote3object.temperature)], true, true);
                          });
                            
                            
                            
                            
                        }, 5000);
                    }
                }
            },
            title: {
                text: 'Real-Time Temperatures'
            },
            xAxis: {
                type: 'datetime',
                tickPixelInterval: 150
            },
            yAxis: {
                title: {
                    text: 'Temperature (F)'
                },
                plotLines: [{
                    value: 0,
                    width: 1,
                    color: '#808080'
                }]
            },
            tooltip: {
                formatter: function() {
                        return '<b>'+ this.series.name +'</b><br/>'+
                        Highcharts.dateFormat('%Y-%m-%d %H:%M:%S', this.x) +'<br/>'+
                        Highcharts.numberFormat(this.y, 2);
                }
            },
            legend: {
                enabled: false
            },
            exporting: {
                enabled: false
            },
            series: [{
                name: 'Mote1',
                data: (function() {
                    // generate an array of random data
                    var data = [],
                        time = (new Date()).getTime(),
                        i;
    
                    for (i = -20; i <= -10; i++) {
                      console.log("pushed time: " + new Date((time + i * 60000)) );
                        data.push({
                            x: time + i * 10000,
                            y: null
                        });
                    }
                    return data;
                })()
            },{
                name: 'Mote2',
                data: (function() {
                    // generate an array of random data
                    var data = [],
                        time = (new Date()).getTime(),
                        i;
    
                    for (i = -20; i <= -10; i++) {
                        data.push({
                            x: time + i * 10000,
                            y: null
                        });
                    }
                    return data;
                })()
            },{
                name: 'Mote3',
                data: (function() {
                    // generate an array of random data
                    var data = [],
                        time = (new Date()).getTime(),
                        i;
    
                    for (i = -20; i <= -10; i++) {
                        data.push({
                            x: time + i * 1000,
                            y: null
                        });
                    }
                    return data;
                })()
            }]
        });
    });
    
});
                </script>
  </head>
  <body>
    <div id="container" style="min-width: 310px; height: 400px; margin: 0 auto"></div>
    <h1>
      <div id="testme">${test1}</div></h1>
    <di
  </body>
</html>

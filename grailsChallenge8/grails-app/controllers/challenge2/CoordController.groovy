package challenge2
import grails.converters.JSON
import com.ece.group7.DatabaseReaderHelper

class CoordController {

    def index() {
    render(view:'index')
    //render(view:'index',model:[test1:"hi"])
    }
    def nexttime1 () {
        
        DatabaseReaderHelper helper = new DatabaseReaderHelper();
        //render();
        ArrayList<Float> collection =helper.getCoords(1);
        //render(lastTemp);
        Float x = collection.get(0);
        Float y = collection.get(collection.size()-1);
        String output = new String(x+" "  +y);
        render(output);
    }
 
}
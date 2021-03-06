package flat.widget.text;

import flat.events.ActionEvent;
import flat.events.ActionListener;
import flat.uxml.Controller;
import flat.uxml.UXStyleAttrs;

public class ToggleButton extends Button {

    private ActionListener toggleListener;

    @Override
    public void applyAttributes(UXStyleAttrs style, Controller controller) {
        super.applyAttributes(style, controller);

        setActivated(style.asBool("activated", isActivated()));
    }

    public ActionListener getToggleListener() {
        return toggleListener;
    }

    public void setToggleListener(ActionListener toggleListener) {
        this.toggleListener = toggleListener;
    }

    public void fireToggle(ActionEvent event) {
        if (toggleListener != null) {
            toggleListener.handle(event);
        }
    }

    public void toggle() {
        setActivated(!isActivated());
    }

    @Override
    public void fireAction(ActionEvent actionEvent) {
        super.fireAction(actionEvent);
        toggle();
    }

    @Override
    public void setActivated(boolean actived) {
        if (this.isActivated() != actived) {
            super.setActivated(actived);
            fireToggle(new ActionEvent(this, ActionEvent.ACTION));
        }
    }

}

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:handover/utils/sizeconfig.dart';

class Sharing extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    SizeConfig().init(context);
    return Scaffold(
      body: AnnotatedRegion<SystemUiOverlayStyle>(
        value: SystemUiOverlayStyle.light,
        child: GestureDetector(
          onTap: () => FocusScope.of(context).unfocus(),
          child: Stack(
            children: <Widget>[
              Container(
                padding: EdgeInsets.symmetric(
                  horizontal: SizeConfig.widthFactor * 40.0,
                ),
                height: double.infinity,
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: <Widget>[
                    SizedBox(
                      height: SizeConfig.heightFactor * 30,
                    ),
                    _buildSendBtn(),
                    _buildReceiveBtn()
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
      resizeToAvoidBottomPadding: false,
    );
  }


  Widget _buildSendBtn() {
    return Container(
        padding: EdgeInsets.symmetric(vertical: SizeConfig.heightFactor * 25.0),
        width: double.infinity,
        child: RaisedButton(
          elevation: SizeConfig.widthFactor * 5.0,
          padding: EdgeInsets.all(SizeConfig.aspectRation * 15.0),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(SizeConfig.aspectRation * 30.0),
          ),
          color: Colors.amber,
          child: Text(
            'Send',
            style: TextStyle(
              color: Colors.black,
              letterSpacing: SizeConfig.widthFactor * 1.5,
              fontSize: SizeConfig.widthFactor * 18.0,
              fontWeight: FontWeight.bold,
              fontFamily: 'OpenSans',
            ),
          ),
          onPressed: null,
        ));
  }

  Widget _buildReceiveBtn() {
    return Container(
        padding: EdgeInsets.symmetric(vertical: SizeConfig.heightFactor * 25.0),
        width: double.infinity,
        child: RaisedButton(
          elevation: SizeConfig.widthFactor * 5.0,
          padding: EdgeInsets.all(SizeConfig.aspectRation * 15.0),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(SizeConfig.aspectRation * 30.0),
          ),
          color: Colors.amber,
          child: Text(
            'Receive',
            style: TextStyle(
              color: Colors.black,
              letterSpacing: SizeConfig.widthFactor * 1.5,
              fontSize: SizeConfig.widthFactor * 18.0,
              fontWeight: FontWeight.bold,
              fontFamily: 'OpenSans',
            ),
          ),
          onPressed: null,
        ));
  }

}